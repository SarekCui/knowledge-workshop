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
import com.knowledge.points.season.service.SeasonSettlementService;
import com.knowledge.points.season.service.SeasonManagementService;
import com.knowledge.points.season.dto.SeasonCreateDTO;
import com.knowledge.points.season.enums.SeasonStatus;
import com.knowledge.common.exception.BusinessException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
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
    SeasonManagementService seasonManagementService;
    @Autowired
    SeasonSettlementService settlementService;

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
        jdbcTemplate.update("DELETE FROM job_execution");
        jdbcTemplate.update("DELETE FROM season");
        jdbcTemplate.update("DELETE FROM rejected_point_event");
        jdbcTemplate.update("DELETE FROM season_archive");
        jdbcTemplate.update("DELETE FROM season_snapshot");
        jdbcTemplate.update("DELETE FROM season_account");
        jdbcTemplate.update("DELETE FROM point_account");
        jdbcTemplate.update("DELETE FROM point_task");
        jdbcTemplate.update("DELETE FROM signin_record");
        jdbcTemplate.update("DELETE FROM point_ledger_2026_q3");
        jdbcTemplate.update("DELETE FROM point_ledger_2026_q2");
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
    }

    @Test
    void adminCanCreateAndQueryQuarterSeasonsWithIdempotencyAndAuthorization() {
        HttpHeaders headers = bearerHeaders("ADMIN");
        String path = "/api/points/admin/seasons";
        Map<String, String> body = Map.of("season", "2026-Q3", "name", "第三季度");
        var created = restTemplate.exchange(path, HttpMethod.POST, new HttpEntity<>(body, headers), JsonNode.class);
        var replay = restTemplate.exchange(path, HttpMethod.POST, new HttpEntity<>(body, headers), JsonNode.class);
        assertThat(created.getStatusCode().value()).isEqualTo(200);
        assertThat(replay.getBody().path("data")).isEqualTo(created.getBody().path("data"));
        assertThat(created.getBody().path("data").path("endsAt").asText()).isEqualTo("2026-10-01T00:00:00Z");
        var conflict = restTemplate.exchange(path, HttpMethod.POST,
                new HttpEntity<>(Map.of("season", "2026-Q3", "name", "不同名称"), headers), JsonNode.class);
        assertThat(conflict.getStatusCode().value()).isEqualTo(409);
        var detail = restTemplate.exchange(path + "/2026-Q3", HttpMethod.GET,
                new HttpEntity<>(headers), JsonNode.class);
        assertThat(detail.getBody().path("data").path("name").asText()).isEqualTo("第三季度");
        var page = restTemplate.exchange(path + "?pageNo=1&pageSize=1", HttpMethod.GET,
                new HttpEntity<>(headers), JsonNode.class);
        assertThat(page.getBody().path("data").path("total").asInt()).isEqualTo(1);
        assertThat(page.getBody().path("data").path("items").size()).isEqualTo(1);
        var forbidden = restTemplate.exchange(path, HttpMethod.GET,
                new HttpEntity<>(bearerHeaders("USER")), JsonNode.class);
        assertThat(forbidden.getStatusCode().value()).isEqualTo(403);
        var invalidPage = restTemplate.exchange(path + "?pageSize=101", HttpMethod.GET,
                new HttpEntity<>(headers), JsonNode.class);
        assertThat(invalidPage.getStatusCode().value()).isEqualTo(400);
        var invalidSeason = restTemplate.exchange(path, HttpMethod.POST,
                new HttpEntity<>(Map.of("season", "2026-Q5", "name", "非法赛季"), headers), JsonNode.class);
        assertThat(invalidSeason.getStatusCode().value()).isEqualTo(400);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM season", Long.class)).isEqualTo(1);
    }

    private HttpHeaders bearerHeaders(String role) {
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder().issuer("knowledge-iam").subject("season-admin-test")
                .issuedAt(now).expiresAt(now.plusSeconds(300)).claim("roles", List.of(role)).build();
        var key = new SecretKeySpec("local-test-secret-at-least-32-bytes-long".getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        var encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    @Test
    void concurrentSettlementUsesDatabaseRankingAndRetainsLateEvents() throws Exception {
        seasonManagementService.create(new SeasonCreateDTO("2026-Q2", "第二季度"));
        var first = pastEvent("past-a", "user-a", 70);
        consumer.consume(objectMapper.writeValueAsString(first));
        consumer.consume(objectMapper.writeValueAsString(pastEvent("past-b", "user-b", 50)));
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
        var executor = Executors.newFixedThreadPool(2);
        try {
            var a = executor.submit(() -> settlementService.settle("2026-Q2"));
            var b = executor.submit(() -> settlementService.settle("2026-Q2"));
            var result = a.get(10, TimeUnit.SECONDS);
            assertThat(b.get(10, TimeUnit.SECONDS)).isEqualTo(result);
            assertThat(result.status()).isEqualTo(SeasonStatus.SETTLED);
            assertThat(result.snapshotCount()).isEqualTo(2);
        } finally {
            executor.shutdownNow();
        }
        assertThat(count("SELECT COUNT(*) FROM season_snapshot WHERE season='2026-Q2'")).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("SELECT user_id FROM season_snapshot WHERE season='2026-Q2' AND rank_no=1", String.class))
                .isEqualTo("user-a");
        assertThat(count("SELECT COUNT(*) FROM season_archive WHERE season='2026-Q2'")).isEqualTo(1);
        consumer.consume(objectMapper.writeValueAsString(first));
        String late = objectMapper.writeValueAsString(pastEvent("late-event", "user-a", 100));
        consumer.consume(late);
        consumer.consume(late);
        assertThat(count("SELECT COUNT(*) FROM rejected_point_event")).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM point_ledger_2026_q2")).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("SELECT score FROM season_snapshot WHERE season='2026-Q2' AND user_id='user-a'", Long.class))
                .isEqualTo(70);
        var response = restTemplate.exchange("/api/points/admin/seasons/2026-Q2/settlement", HttpMethod.POST,
                new HttpEntity<>(bearerHeaders("ADMIN")), JsonNode.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().path("data").path("status").asText()).isEqualTo("SETTLED");
        var history = restTemplate.exchange("/api/points/admin/seasons/2026-Q2/ranking?pageNo=2&pageSize=1", HttpMethod.GET,
                new HttpEntity<>(bearerHeaders("ADMIN")), JsonNode.class);
        assertThat(history.getStatusCode().value()).isEqualTo(200);
        assertThat(history.getBody().path("data").path("total").asInt()).isEqualTo(2);
        assertThat(history.getBody().path("data").path("items").get(0).path("rank").asInt()).isEqualTo(2);
        assertThat(history.getBody().path("data").path("items").get(0).path("userId").asText()).isEqualTo("user-b");
        assertThat(maintenanceService.snapshot("2026-Q2", 1000)).isZero();
        assertThat(maintenanceService.archive(tableRouter.route(first.occurredAt()))).isFalse();
        var forbidden = restTemplate.exchange("/api/points/admin/seasons/2026-Q2/settlement", HttpMethod.POST,
                new HttpEntity<>(bearerHeaders("USER")), JsonNode.class);
        assertThat(forbidden.getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void outstandingPublishedTaskBlocksSettlementUntilConsumerAppliesIt() throws Exception {
        seasonManagementService.create(new SeasonCreateDTO("2026-Q2", "第二季度"));
        var event = new PointGrantEventDTO("quarter-start", "pending-user", 20, "SIGN_IN", "quarter-start",
                "quarter-start", Instant.parse("2026-04-01T00:00:00.001Z"), 1);
        String payload = objectMapper.writeValueAsString(event);
        jdbcTemplate.update("""
                INSERT INTO point_task(id,event_id,payload,status,retry_count,next_retry_at,created_at,updated_at,occurred_at)
                VALUES('pending-id','quarter-start',?,'SENT',0,UTC_TIMESTAMP(),UTC_TIMESTAMP(),UTC_TIMESTAMP(),'2026-04-01 00:00:00')
                """, payload);
        assertThatThrownBy(() -> settlementService.settle("2026-Q2"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("未入账");
        assertThat(seasonManagementService.get("2026-Q2").status()).isEqualTo(SeasonStatus.FAILED);
        assertThat(count("SELECT COUNT(*) FROM season_snapshot")).isZero();
        assertThat(count("SELECT COUNT(*) FROM season_archive")).isZero();
        consumer.consume(payload);
        var result = settlementService.settle("2026-Q2");
        assertThat(result.status()).isEqualTo(SeasonStatus.SETTLED);
        assertThat(result.lastError()).isNull();
        assertThat(result.snapshotCount()).isEqualTo(1);
    }

    @Test
    void settlementFailureRollsBackSnapshotAndArchiveAndCanRetry() throws Exception {
        seasonManagementService.create(new SeasonCreateDTO("2026-Q2", "第二季度"));
        consumer.consume(objectMapper.writeValueAsString(pastEvent("rollback-event", "rollback-user", 10)));
        jdbcTemplate.execute("ALTER TABLE season_archive ADD CONSTRAINT reject_archive CHECK (season <> '2026-Q2')");
        try {
            assertThatThrownBy(() -> settlementService.settle("2026-Q2")).isInstanceOf(RuntimeException.class);
            assertThat(seasonManagementService.get("2026-Q2").status()).isEqualTo(SeasonStatus.FAILED);
            assertThat(count("SELECT COUNT(*) FROM season_snapshot")).isZero();
            assertThat(count("SELECT COUNT(*) FROM season_archive")).isZero();
        } finally {
            jdbcTemplate.execute("ALTER TABLE season_archive DROP CHECK reject_archive");
        }
        assertThat(settlementService.settle("2026-Q2").status()).isEqualTo(SeasonStatus.SETTLED);
    }

    private PointGrantEventDTO pastEvent(String eventId, String userId, int points) {
        return new PointGrantEventDTO(eventId, userId, points, "SIGN_IN", eventId, eventId,
                Instant.parse("2026-05-01T00:00:00Z"), 1);
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
            assertThat(count("SELECT COUNT(*) FROM point_ledger_2026_q3 WHERE event_id = 'duplicate-event'"))
                    .isEqualTo(1);
            assertThat(count("SELECT total_points FROM point_account WHERE user_id = 'user-duplicate'"))
                    .isEqualTo(20);
            assertThat(count("SELECT points FROM season_account "
                    + "WHERE season = '2026-Q3' AND user_id = 'user-duplicate'"))
                    .isEqualTo(20);
        });
    }

    @Test
    void leaderboardCanBeRebuiltAndSeasonJobsAreIdempotent() throws Exception {
        consumer.consume(objectMapper.writeValueAsString(event("event-a", "user-a", 30)));
        consumer.consume(objectMapper.writeValueAsString(event("event-b", "user-b", 50)));
        consumer.consume(objectMapper.writeValueAsString(event("event-c", "user-a", 40)));
        redisTemplate.delete("kw:points:ranking:season:" + route.season());

        assertThat(leaderboardService.top(route.season(), 10)).isEmpty();
        assertThat(rebuildService.rebuild(route)).isEqualTo(2);
        assertThat(leaderboardService.top(route.season(), 10))
                .extracting(item -> item.userId() + ':' + item.score())
                .containsExactly("user-a:70", "user-b:50");

        assertThat(maintenanceService.snapshot(route.season(), 10)).isEqualTo(2);
        assertThat(maintenanceService.snapshot(route.season(), 10)).isZero();
        assertThat(count("SELECT COUNT(*) FROM season_snapshot WHERE season = '2026-Q3'"))
                .isEqualTo(2);

        assertThat(maintenanceService.archive(route)).isTrue();
        assertThat(maintenanceService.archive(route)).isFalse();
        assertThat(count("SELECT COUNT(*) FROM season_archive WHERE season = '2026-Q3'"))
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
        assertThat(document.path("paths").has("/api/points/admin/seasons/{season}/settlement")).isTrue();
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
