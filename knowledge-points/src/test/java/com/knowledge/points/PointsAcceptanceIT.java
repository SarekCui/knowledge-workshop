package com.knowledge.points;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.knowledge.api.points.dto.PointGrantEventDTO;
import com.knowledge.points.ranking.config.PointsRabbitConfiguration;
import com.knowledge.points.ranking.service.LeaderboardRebuildService;
import com.knowledge.points.ranking.service.LeaderboardService;
import com.knowledge.points.ranking.service.PointGrantConsumer;
import com.knowledge.points.ranking.bo.QuarterTableRouteBO;
import com.knowledge.points.ranking.service.QuarterTableRouter;
import com.knowledge.points.season.service.SeasonMaintenanceService;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.cloud.nacos.discovery.enabled=false",
        "knowledge.points.task.fixed-delay=3600000"
})
class PointsAcceptanceIT {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("knowledge_points");

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine"))
            .withExposedPorts(6379);

    @Container
    static final RabbitMQContainer RABBIT = new RabbitMQContainer("rabbitmq:4.1-management-alpine");

    @DynamicPropertySource
    static void infrastructure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("spring.rabbitmq.host", RABBIT::getHost);
        registry.add("spring.rabbitmq.port", RABBIT::getAmqpPort);
        registry.add("spring.rabbitmq.username", RABBIT::getAdminUsername);
        registry.add("spring.rabbitmq.password", RABBIT::getAdminPassword);
        registry.add("knowledge.security.jwt.secret", () -> "local-test-secret-at-least-32-bytes-long");
    }

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    RabbitAdmin rabbitAdmin;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    PointGrantConsumer consumer;

    @Autowired
    LeaderboardService leaderboardService;

    @Autowired
    LeaderboardRebuildService rebuildService;

    @Autowired
    SeasonMaintenanceService maintenanceService;

    @Autowired
    QuarterTableRouter tableRouter;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    TestRestTemplate restTemplate;

    private final QuarterTableRouteBO route = new QuarterTableRouter().route(Instant.parse("2026-08-01T00:00:00Z"));

    @BeforeEach
    void prepare() {
        rabbitAdmin.purgeQueue(PointsRabbitConfiguration.POINT_GRANT_QUEUE, true);
        jdbcTemplate.update("DELETE FROM pt_job_execution");
        jdbcTemplate.update("DELETE FROM pt_season_archive");
        jdbcTemplate.update("DELETE FROM pt_season_snapshot");
        jdbcTemplate.update("DELETE FROM pt_season_account");
        jdbcTemplate.update("DELETE FROM pt_point_account");
        jdbcTemplate.update("DELETE FROM pt_point_task");
        jdbcTemplate.update("DELETE FROM pt_signin_record");
        jdbcTemplate.update("DELETE FROM pt_point_ledger_2026_q3");
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
    }

    @Test
    void tenDuplicateRabbitMessagesCreateOneLedgerEntry() throws Exception {
        PointGrantEventDTO event = event("duplicate-event", "user-duplicate", 20);
        String payload = objectMapper.writeValueAsString(event);
        for (int i = 0; i < 10; i++) {
            rabbitTemplate.convertAndSend(PointsRabbitConfiguration.EVENT_EXCHANGE,
                    PointsRabbitConfiguration.POINT_GRANT_ROUTING_KEY, payload);
        }

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            assertThat(count("SELECT COUNT(*) FROM pt_point_ledger_2026_q3 WHERE event_id = 'duplicate-event'"))
                    .isEqualTo(1);
            assertThat(count("SELECT total_points FROM pt_point_account WHERE user_id = 'user-duplicate'"))
                    .isEqualTo(20);
            assertThat(count("SELECT points FROM pt_season_account "
                    + "WHERE season = '2026-Q3' AND user_id = 'user-duplicate'"))
                    .isEqualTo(20);
        });
    }

    @Test
    void leaderboardCanBeRebuiltAndSeasonJobsAreIdempotent() throws Exception {
        consumer.consume(objectMapper.writeValueAsString(event("event-a", "user-a", 30)));
        consumer.consume(objectMapper.writeValueAsString(event("event-b", "user-b", 50)));
        consumer.consume(objectMapper.writeValueAsString(event("event-c", "user-a", 40)));
        redisTemplate.delete("kw:points:ranking:" + route.season());

        assertThat(leaderboardService.top(route.season(), 10)).isEmpty();
        assertThat(rebuildService.rebuild(route)).isEqualTo(2);
        assertThat(leaderboardService.top(route.season(), 10))
                .extracting(item -> item.userId() + ':' + item.score())
                .containsExactly("user-a:70", "user-b:50");

        assertThat(maintenanceService.snapshot(route.season(), 10)).isEqualTo(2);
        assertThat(maintenanceService.snapshot(route.season(), 10)).isZero();
        assertThat(count("SELECT COUNT(*) FROM pt_season_snapshot WHERE season = '2026-Q3'"))
                .isEqualTo(2);

        assertThat(maintenanceService.archive(route)).isTrue();
        assertThat(maintenanceService.archive(route)).isFalse();
        assertThat(count("SELECT COUNT(*) FROM pt_season_archive WHERE season = '2026-Q3'"))
                .isEqualTo(1);
    }

    @Test
    void openApiContractDocumentsPointsOperationsAndSchemas() {
        JsonNode document = restTemplate.getForObject("/v3/api-docs", JsonNode.class);

        assertThat(document.path("info").path("title").asText())
                .isEqualTo("Knowledge Workshop Points API");
        JsonNode signIn = document.path("paths").path("/api/points/sign-ins").path("post");
        assertThat(signIn.isMissingNode()).isFalse();
        assertThat(signIn.path("responses").has("200")).isTrue();
        assertThat(signIn.path("parameters").toString()).contains("X-Request-Id");
        assertThat(document.path("paths").has("/api/points/leaderboard")).isTrue();
        assertThat(document.path("components").path("schemas").has("LeaderboardItemVO")).isTrue();
    }

    private PointGrantEventDTO event(String eventId, String userId, int points) {
        return new PointGrantEventDTO(eventId, userId, points, "SIGN_IN", eventId,
                eventId, Instant.parse("2026-08-01T00:00:00Z"), 1);
    }

    private long count(String sql) {
        return jdbcTemplate.queryForObject(sql, Long.class);
    }
}
