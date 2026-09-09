package com.knowledge.learning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledge.api.learning.dto.PlayedRangeEventDTO;
import com.knowledge.api.learning.dto.VideoProgressReportedEventDTO;
import com.knowledge.api.marketing.dto.GroupFormedEventDTO;
import com.knowledge.common.exception.BusinessException;
import com.knowledge.learning.entitlement.config.LearningRabbitConfiguration;
import com.knowledge.learning.note.bo.NoteBO;
import com.knowledge.learning.note.dto.CreateNoteDTO;
import com.knowledge.learning.note.dto.RenameNoteDTO;
import com.knowledge.learning.note.service.NoteService;
import com.knowledge.learning.progress.bo.PlaybackSessionBO;
import com.knowledge.learning.progress.bo.VideoProgressBO;
import com.knowledge.learning.progress.dto.PlayedRangeDTO;
import com.knowledge.learning.progress.dto.ReportProgressDTO;
import com.knowledge.learning.progress.enums.ProgressEventType;
import com.knowledge.learning.progress.enums.ProgressStatus;
import com.knowledge.learning.progress.service.PlaybackSessionService;
import com.knowledge.learning.progress.service.ProgressCacheService;
import com.knowledge.learning.progress.service.ProgressQueryService;
import com.knowledge.learning.progress.service.ProgressService;
import com.knowledge.learning.progress.service.ProgressTransactionService;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
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
        "spring.rabbitmq.listener.simple.retry.initial-interval=10ms",
        "spring.rabbitmq.listener.simple.retry.max-interval=20ms"
})
class LearningAcceptanceIT {

    private static final String JWT_SECRET = "local-test-secret-at-least-32-bytes-long";

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("knowledge_learning");

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
        registry.add("knowledge.security.jwt.secret", () -> JWT_SECRET);
    }

    @Autowired
    NoteService noteService;

    @Autowired
    PlaybackSessionService sessionService;

    @Autowired
    ProgressService progressService;

    @Autowired
    ProgressQueryService progressQueryService;

    @Autowired
    ProgressTransactionService transactionService;

    @Autowired
    ProgressCacheService cacheService;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    RabbitAdmin rabbitAdmin;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    TestRestTemplate restTemplate;

    @BeforeEach
    void prepare() {
        rabbitAdmin.purgeQueue(LearningRabbitConfiguration.GROUP_FORMED_QUEUE, true);
        rabbitAdmin.purgeQueue(LearningRabbitConfiguration.PROGRESS_QUEUE, true);
        rabbitAdmin.purgeQueue(LearningRabbitConfiguration.GROUP_FORMED_DEAD_QUEUE, true);
        rabbitAdmin.purgeQueue(LearningRabbitConfiguration.PROGRESS_DEAD_QUEUE, true);
        jdbcTemplate.update("DELETE FROM lr_progress_event_inbox");
        jdbcTemplate.update("DELETE FROM lr_watched_segment");
        jdbcTemplate.update("DELETE FROM lr_video_progress");
        jdbcTemplate.update("DELETE FROM lr_message_inbox");
        jdbcTemplate.update("DELETE FROM lr_course_entitlement");
        jdbcTemplate.update("DELETE FROM lr_note");
        jdbcTemplate.update("DELETE FROM lr_chapter");
        jdbcTemplate.update("DELETE FROM lr_course");
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
        insertCourseAndChapter();
        insertEntitlement("user-a");
    }

    @Test
    void noteSupportsIdempotentCreateRenameOwnershipAndOptimisticConflict() {
        CreateNoteDTO create = new CreateNoteDTO("note-request-1", "course-java", "chapter-java-1",
                "  Redis 名额控制  ", "使用原子命令", 20_000L);
        NoteBO first = noteService.create("user-a", create);
        NoteBO duplicate = noteService.create("user-a", create);

        assertThat(duplicate.id()).isEqualTo(first.id());
        assertThat(first.title()).isEqualTo("Redis 名额控制");

        NoteBO renamed = noteService.rename("user-a", first.id(), new RenameNoteDTO("Lua 原子占位", 0));
        assertThat(renamed.title()).isEqualTo("Lua 原子占位");
        assertThat(renamed.version()).isEqualTo(1);
        assertThatThrownBy(() -> noteService.rename(
                "user-a", first.id(), new RenameNoteDTO("过期修改", 0)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("其他设备修改");
        assertThatThrownBy(() -> noteService.get("user-b", first.id()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权");
        assertThatThrownBy(() -> noteService.create("user-b", new CreateNoteDTO(
                "note-request-no-right", "course-java", "chapter-java-1", "无权益", "内容", 0L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("尚未获得");
    }

    @Test
    void duplicatedGroupFormedEventGrantsEachUserExactlyOneEntitlement() throws Exception {
        GroupFormedEventDTO event = new GroupFormedEventDTO("group-event-1", "group-1", "activity-1",
                "course-java", List.of("user-b", "user-c", "user-b"), "request-1", Instant.now(), 1);
        String payload = objectMapper.writeValueAsString(event);

        rabbitTemplate.convertAndSend(LearningRabbitConfiguration.EVENT_EXCHANGE,
                LearningRabbitConfiguration.GROUP_FORMED_ROUTING_KEY, payload);
        rabbitTemplate.convertAndSend(LearningRabbitConfiguration.EVENT_EXCHANGE,
                LearningRabbitConfiguration.GROUP_FORMED_ROUTING_KEY, payload);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertThat(count("SELECT COUNT(*) FROM lr_course_entitlement WHERE source_id = 'group-1'"))
                    .isEqualTo(2);
            assertThat(count("SELECT COUNT(*) FROM lr_message_inbox WHERE event_id = 'group-event-1'"))
                    .isEqualTo(1);
        });
    }

    @Test
    void poisonMessagesAreRetriedThenRetainedInDeadLetterQueues() {
        rabbitTemplate.convertAndSend(LearningRabbitConfiguration.EVENT_EXCHANGE,
                LearningRabbitConfiguration.GROUP_FORMED_ROUTING_KEY, "not-json");
        rabbitTemplate.convertAndSend(LearningRabbitConfiguration.EVENT_EXCHANGE,
                LearningRabbitConfiguration.PROGRESS_ROUTING_KEY, "not-json");

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            assertThat(rabbitAdmin.getQueueInfo(LearningRabbitConfiguration.GROUP_FORMED_DEAD_QUEUE)
                    .getMessageCount()).isEqualTo(1);
            assertThat(rabbitAdmin.getQueueInfo(LearningRabbitConfiguration.PROGRESS_DEAD_QUEUE)
                    .getMessageCount()).isEqualTo(1);
        });
    }

    @Test
    void duplicatedProgressEventCountsWatchedSegmentsOnceAndCompletesVideo() {
        PlaybackSessionBO session = sessionService.start("user-a", "video-java-1");
        ReportProgressDTO report = report("progress-event-1", session, 1, 90_000,
                ProgressEventType.ENDED, List.of(new PlayedRangeDTO(0, 90_000)));

        for (int i = 0; i < 10; i++) {
            progressService.report("user-a", "video-java-1", report);
        }

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            VideoProgressBO progress = progressQueryService.get("user-a", "video-java-1");
            assertThat(progress.status()).isEqualTo(ProgressStatus.COMPLETED);
            assertThat(progress.completionRate()).isEqualTo(9000);
            assertThat(progress.watchedSeconds()).isEqualTo(90);
            assertThat(count("SELECT COUNT(*) FROM lr_progress_event_inbox WHERE event_id = 'progress-event-1'"))
                    .isEqualTo(1);
            assertThat(count("SELECT COUNT(*) FROM lr_watched_segment")).isEqualTo(9);
        });
    }

    @Test
    void sequenceAndSessionEpochPreventStaleOverwriteButCurrentSessionCanRewind() {
        long epoch = 100;
        transactionService.process(event("event-seq-20", epoch, 20, 60_000, List.of()));
        transactionService.process(event("event-seq-19", epoch, 19, 20_000, List.of()));
        VideoProgressBO afterOutOfOrder = progressQueryService.get("user-a", "video-java-1");
        assertThat(afterOutOfOrder.resumePositionMs()).isEqualTo(60_000);

        transactionService.process(event("event-seq-21", epoch, 21, 30_000, List.of()));
        cacheService.clearProgress("user-a", "video-java-1", 1);
        VideoProgressBO afterRewind = progressQueryService.get("user-a", "video-java-1");
        assertThat(afterRewind.resumePositionMs()).isEqualTo(30_000);

        PlaybackSessionBO oldSession = sessionService.start("user-a", "video-java-1");
        PlaybackSessionBO newSession = sessionService.start("user-a", "video-java-1");
        assertThat(newSession.sessionEpoch()).isGreaterThan(oldSession.sessionEpoch());
        assertThatThrownBy(() -> progressService.report("user-a", "video-java-1",
                report("old-session-event", oldSession, 1, 40_000, ProgressEventType.PAUSE, List.of())))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("新设备替代");
    }

    @Test
    void redisLossFallsBackToMysqlAndHttpContractIsSecured() {
        transactionService.process(event("database-event", 200, 1, 45_000,
                List.of(new PlayedRangeEventDTO(0, 40_000))));
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();

        VideoProgressBO restored = progressQueryService.get("user-a", "video-java-1");
        assertThat(restored.resumePositionMs()).isEqualTo(45_000);
        assertThat(redisTemplate.hasKey("kw:learning:progress:user-a:video-java-1:1")).isTrue();

        JsonNode openApi = restTemplate.getForObject("/v3/api-docs", JsonNode.class);
        assertThat(openApi.path("info").path("title").asText())
                .isEqualTo("Knowledge Workshop Learning API");
        assertThat(openApi.path("paths").has("/api/learning/notes")).isTrue();
        assertThat(openApi.path("paths").has("/api/learning/videos/{videoId}/progress")).isTrue();

        ResponseEntity<JsonNode> unauthenticated = restTemplate.getForEntity(
                "/api/learning/courses", JsonNode.class);
        assertThat(unauthenticated.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        HttpHeaders learnerHeaders = new HttpHeaders();
        learnerHeaders.setBearerAuth(accessToken("user-a", "LEARNER"));
        ResponseEntity<JsonNode> forbidden = restTemplate.exchange(
                "/api/learning/admin/courses", HttpMethod.POST,
                new HttpEntity<>(new com.knowledge.learning.course.dto.CreateCourseDTO(
                        "课程", "简介", null, 0L), learnerHeaders), JsonNode.class);
        assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private ReportProgressDTO report(String eventId, PlaybackSessionBO session, long sequence, long position,
                                     ProgressEventType type, List<PlayedRangeDTO> ranges) {
        return new ReportProgressDTO(eventId, session.sessionId(), session.sessionEpoch(), sequence, type,
                position, ranges, Instant.now(), BigDecimal.ONE);
    }

    private VideoProgressReportedEventDTO event(String eventId, long epoch, long sequence, long position,
                                                List<PlayedRangeEventDTO> ranges) {
        return new VideoProgressReportedEventDTO(eventId, "user-a", "course-java", "chapter-java-1",
                "video-java-1", 1, "session-direct", epoch, sequence, "PAUSE", position, 100_000,
                ranges, Instant.now(), Instant.now(), 1);
    }

    private void insertCourseAndChapter() {
        jdbcTemplate.update("""
                INSERT INTO lr_course
                  (id, title, summary, price_cents, status, version, created_at, updated_at)
                VALUES ('course-java', 'Java', 'Java course', 9900, 'PUBLISHED', 0,
                        UTC_TIMESTAMP(3), UTC_TIMESTAMP(3))
                """);
        jdbcTemplate.update("""
                INSERT INTO lr_chapter
                  (id, course_id, title, sort_order, video_id, video_url, video_duration_ms,
                   video_version, status, version, created_at, updated_at)
                VALUES ('chapter-java-1', 'course-java', 'Chapter 1', 1, 'video-java-1',
                        'https://example.invalid/video.m3u8', 100000, 1, 'PUBLISHED', 0,
                        UTC_TIMESTAMP(3), UTC_TIMESTAMP(3))
                """);
    }

    private void insertEntitlement(String userId) {
        jdbcTemplate.update("""
                INSERT INTO lr_course_entitlement
                  (id, user_id, course_id, source_type, source_id, status,
                   effective_at, created_at, updated_at)
                VALUES (?, ?, 'course-java', 'TEST', ?, 'ACTIVE',
                        UTC_TIMESTAMP(3), UTC_TIMESTAMP(3), UTC_TIMESTAMP(3))
                """, "entitlement-" + userId, userId, "test-" + userId);
    }

    private String accessToken(String userId, String role) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("knowledge-iam")
                .subject(userId)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .claim("username", "acceptance-user")
                .claim("roles", List.of(role))
                .build();
        SecretKeySpec key = new SecretKeySpec(JWT_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        return encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    private long count(String sql) {
        return jdbcTemplate.queryForObject(sql, Long.class);
    }
}
