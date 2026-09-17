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
import com.knowledge.learning.course.service.CourseCatalogService;
import com.knowledge.learning.note.bo.NoteBO;
import com.knowledge.learning.note.dto.CreateNoteDTO;
import com.knowledge.learning.note.dto.RenameNoteDTO;
import com.knowledge.learning.note.dto.UpdateNoteDTO;
import com.knowledge.learning.note.service.NoteService;
import com.knowledge.learning.note.service.NoteQueryService;
import com.knowledge.learning.note.dao.mapper.NoteImageMapper;
import com.knowledge.learning.note.dto.ChangeNoteStatusDTO;
import com.knowledge.learning.note.enums.NoteStatus;
import com.knowledge.learning.note.enums.NoteSort;
import com.knowledge.learning.note.dto.CreateNoteCommentDTO;
import com.knowledge.learning.note.service.NoteCommentService;
import com.knowledge.learning.note.service.NoteEngagementService;
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
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
        "knowledge.storage.enabled=false",
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
    NoteQueryService noteQueryService;

    @Autowired
    NoteEngagementService noteEngagementService;

    @Autowired
    NoteCommentService noteCommentService;

    @Autowired
    NoteImageMapper noteImageMapper;

    @Autowired
    CourseCatalogService courseCatalogService;

    @Test
    void noteSharingSupportsOptionalCourseAndPrivatePublicBoundaries() {
        var input = new CreateNoteDTO("share-1", null, null, "独立知识", "正文关键词_100%", null);
        var draft = noteService.create("user-b", input);
        assertThat(draft.courseId()).isNull();
        assertThat(draft.status()).isEqualTo(NoteStatus.DRAFT);
        assertThat(noteService.create("user-b", input).id()).isEqualTo(draft.id());
        assertThat(noteQueryService.publicPage(null, null, NoteSort.LATEST, 1, 20).total()).isZero();
        assertThat(noteQueryService.mine("user-a", null, null, 1, 20).total()).isZero();
        assertThatThrownBy(() -> noteQueryService.getPublic(draft.id())).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> noteService.changeStatus("user-a", draft.id(),
                new ChangeNoteStatusDTO(NoteStatus.PUBLIC, 0))).isInstanceOf(BusinessException.class);
        var published = noteService.changeStatus("user-b", draft.id(), new ChangeNoteStatusDTO(NoteStatus.PUBLIC, 0));
        assertThat(published.status()).isEqualTo(NoteStatus.PUBLIC);
        assertThat(noteQueryService.publicPage(null, "关键词_100%", NoteSort.LATEST, 1, 20).items())
                .extracting(NoteBO::id).containsExactly(draft.id());
        assertThat(noteQueryService.publicPage("course-java", null, NoteSort.LATEST, 1, 20).total()).isZero();
        assertThat(learnerGet("/api/learning/notes/public/" + draft.id(), "user-a").getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThatThrownBy(() -> noteService.rename("user-b", draft.id(), new RenameNoteDTO("直接修改公开内容", 1)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("草稿");
        assertThatThrownBy(() -> noteService.changeStatus("user-b", draft.id(), new ChangeNoteStatusDTO(NoteStatus.PRIVATE, 0)))
                .isInstanceOf(BusinessException.class);
        noteService.changeStatus("user-b", draft.id(), new ChangeNoteStatusDTO(NoteStatus.PRIVATE, 1));
        assertThat(noteQueryService.publicPage(null, "独立知识", NoteSort.LATEST, 1, 20).total()).isZero();
        assertThat(learnerGet("/api/learning/notes/public/" + draft.id(), "user-a").getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        noteService.delete("user-b", draft.id(), 2);
        assertThat(noteQueryService.mine("user-b", null, null, 1, 20).total()).isZero();
    }

    @Test
    void courseCatalogSupportsFilteringSafeOutlineAndEntitlementAwareActions() {
        var categories = courseCatalogService.categories();
        assertThat(categories).extracting("id").contains("general", "backend", "frontend");

        var entitledPage = courseCatalogService.page("user-a", "backend", "Java", 1, 12);
        assertThat(entitledPage.total()).isEqualTo(1);
        assertThat(entitledPage.items()).singleElement().satisfies(course -> {
            assertThat(course.id()).isEqualTo("course-java");
            assertThat(course.entitled()).isTrue();
            assertThat(course.chapterCount()).isEqualTo(1);
            assertThat(course.totalDurationMs()).isEqualTo(100_000L);
        });
        assertThat(courseCatalogService.page("user-b", "backend", "Java", 1, 12).items())
                .singleElement().extracting("entitled").isEqualTo(false);

        var detail = learnerGet("/api/learning/catalog/courses/course-java", "user-b");
        assertThat(detail.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(detail.getBody().path("data").path("course").path("entitled").asBoolean()).isFalse();
        JsonNode chapter = detail.getBody().path("data").path("chapters").get(0);
        assertThat(chapter.path("title").asText()).isEqualTo("Chapter 1");
        assertThat(chapter.has("videoUrl")).isFalse();

        var guestDetail = restTemplate.getForEntity(
                "/api/learning/catalog/courses/course-java", JsonNode.class);
        assertThat(guestDetail.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(guestDetail.getBody().path("data").path("course").path("entitled").asBoolean()).isFalse();

        assertThat(learnerGet("/api/learning/courses/course-java/chapters", "user-b").getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(learnerGet("/api/learning/courses/course-java/chapters", "user-a").getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    void technicalTagsAreNormalizedPersistedSearchedAndIdempotent() {
        var input = new CreateNoteDTO("tag-request-1", null, null, "分布式锁实践", "锁的实现细节", null,
                List.of(" Java ", "Redis", "java"));
        var draft = noteService.create("user-b", input);
        assertThat(draft.tags()).containsExactly("Java", "Redis");
        assertThat(noteService.create("user-b", input).id()).isEqualTo(draft.id());

        var updated = noteService.update("user-b", draft.id(),
                new UpdateNoteDTO(draft.title(), draft.content(), null, 0, List.of("Spring Boot", "Redisson")));
        assertThat(updated.tags()).containsExactly("Spring Boot", "Redisson");
        var published = noteService.changeStatus("user-b", draft.id(),
                new ChangeNoteStatusDTO(NoteStatus.PUBLIC, updated.version()));

        assertThat(noteQueryService.publicPage(null, null, "spring boot", NoteSort.LATEST, 1, 20).items())
                .extracting(NoteBO::id).containsExactly(published.id());
        assertThat(noteQueryService.publicPage(null, "REDISSON", null, NoteSort.LATEST, 1, 20).items())
                .extracting(NoteBO::id).containsExactly(published.id());
        assertThat(noteQueryService.getPublic(published.id()).tags()).containsExactly("Spring Boot", "Redisson");
        assertThatThrownBy(() -> noteService.create("user-b", new CreateNoteDTO(
                "tag-request-1", null, null, "分布式锁实践", "锁的实现细节", null, List.of("MySQL"))))
                .isInstanceOf(BusinessException.class).hasMessageContaining("clientRequestId");
    }

    @Test
    void noteSharingPreservesPrivateDefaultAndValidatesAssociations() {
        jdbcTemplate.update("""
                INSERT INTO note(id,user_id,client_request_id,title,content,created_at,updated_at)
                VALUES('legacy-private','user-b','legacy-request','原有私人内容','不公开',UTC_TIMESTAMP(3),UTC_TIMESTAMP(3))
                """);
        assertThat(noteService.get("user-b", "legacy-private").status()).isEqualTo(NoteStatus.PRIVATE);
        assertThat(noteQueryService.publicPage(null, null, NoteSort.LATEST, 1, 20).total()).isZero();
        var related = noteService.create("user-b", new CreateNoteDTO("no-purchase", "course-java", null, "课程分享", "内容", null));
        assertThat(related.courseId()).isEqualTo("course-java");
        assertThatThrownBy(() -> noteService.create("user-b", new CreateNoteDTO("bad-chapter", null, "chapter-java-1", "无课程", "内容", null)))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> noteService.create("user-b", new CreateNoteDTO("bad-position", null, null, "无章节", "内容", 1L)))
                .isInstanceOf(BusinessException.class);
        var empty = noteService.create("user-b", new CreateNoteDTO("empty-note", null, null, "空草稿", "", null));
        assertThatThrownBy(() -> noteService.changeStatus("user-b", empty.id(), new ChangeNoteStatusDTO(NoteStatus.PUBLIC, 0)))
                .isInstanceOf(BusinessException.class);
        assertThat(learnerGet("/api/learning/notes/public?pageSize=51", "user-a").getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

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
        jdbcTemplate.update("DELETE FROM progress_event_inbox");
        jdbcTemplate.update("DELETE FROM watched_segment");
        jdbcTemplate.update("DELETE FROM video_progress");
        jdbcTemplate.update("DELETE FROM message_inbox");
        jdbcTemplate.update("DELETE FROM course_entitlement");
        jdbcTemplate.update("DELETE FROM note_comment");
        jdbcTemplate.update("DELETE FROM note_favorite");
        jdbcTemplate.update("DELETE FROM note_like");
        jdbcTemplate.update("DELETE FROM note_image");
        jdbcTemplate.update("DELETE FROM note_tag");
        jdbcTemplate.update("DELETE FROM note");
        jdbcTemplate.update("DELETE FROM chapter");
        jdbcTemplate.update("DELETE FROM course");
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
    void noteImageReferencesBindReleaseAndRollbackWithTheNoteTransaction() {
        String imageId = "11111111-1111-1111-1111-111111111111";
        jdbcTemplate.update("""
                INSERT INTO note_image(id,user_id,object_key,content_type,file_size,width,height,status,
                                       expires_at,version,created_at,updated_at)
                VALUES(?,?,?,'image/png',128,800,600,'TEMP',DATE_ADD(UTC_TIMESTAMP(3), INTERVAL 1 DAY),
                       0,UTC_TIMESTAMP(3),UTC_TIMESTAMP(3))
                """, imageId, "user-b", "note-images/test.png");
        String markdown = "![架构图](/api/learning/note-images/" + imageId + ")";

        var note = noteService.create("user-b",
                new CreateNoteDTO("image-note-request", null, null, "图片生命周期", markdown, null));
        assertThat(jdbcTemplate.queryForMap("SELECT note_id,status,expires_at FROM note_image WHERE id=?", imageId))
                .containsEntry("note_id", note.id())
                .containsEntry("status", "BOUND")
                .containsEntry("expires_at", null);

        noteService.update("user-b", note.id(), new UpdateNoteDTO(note.title(), "移除图片", null, 0));
        assertThat(jdbcTemplate.queryForMap("SELECT note_id,status FROM note_image WHERE id=?", imageId))
                .containsEntry("note_id", null)
                .containsEntry("status", "TEMP");

        assertThatThrownBy(() -> noteService.create("user-a",
                new CreateNoteDTO("foreign-image-request", null, null, "越权引用", markdown, null)))
                .isInstanceOf(BusinessException.class);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM note WHERE client_request_id='foreign-image-request'", Integer.class))
                .isZero();

        jdbcTemplate.update("UPDATE note_image SET expires_at=DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 1 MINUTE) WHERE id=?",
                imageId);
        var now = java.time.LocalDateTime.now(java.time.Clock.systemUTC());
        noteImageMapper.recoverStaleClaims(now.minusMinutes(10), now);
        assertThat(noteImageMapper.claimExpired("acceptance-cleanup", now, 100)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM note_image WHERE id=?", String.class, imageId))
                .isEqualTo("DELETING");
    }

    @Test
    void noteEngagementIsIdempotentCountedAndSupportsOneLevelReplies() {
        var draft = noteService.create("user-b", new CreateNoteDTO(
                "engagement-note", null, null, "互动测试", "真实互动", null));
        var published = noteService.changeStatus("user-b", draft.id(),
                new ChangeNoteStatusDTO(NoteStatus.PUBLIC, 0));

        noteEngagementService.like("user-a", published.id());
        noteEngagementService.like("user-a", published.id());
        noteEngagementService.favorite("user-a", published.id());
        assertThat(noteEngagementService.get("user-a", published.id()))
                .extracting("likeCount", "favoriteCount", "liked", "favorited")
                .containsExactly(1L, 1L, true, true);
        assertThat(noteQueryService.publicPageForUser("user-a", null, null, null,
                NoteSort.LATEST, 1, 20).items())
                .filteredOn(note -> note.id().equals(published.id()))
                .singleElement()
                .extracting(NoteBO::liked, NoteBO::favorited)
                .containsExactly(true, true);
        assertThat(noteQueryService.publicPageForUser("user-b", null, null, null,
                NoteSort.LATEST, 1, 20).items())
                .filteredOn(note -> note.id().equals(published.id()))
                .singleElement()
                .extracting(NoteBO::liked, NoteBO::favorited)
                .containsExactly(false, false);
        var guestPage = restTemplate.getForEntity(
                "/api/learning/notes/public?pageNo=1&pageSize=20", JsonNode.class);
        assertThat(guestPage.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode guestNote = guestPage.getBody().path("data").path("items").findValuesAsText("id").contains(published.id())
                ? java.util.stream.StreamSupport.stream(
                        guestPage.getBody().path("data").path("items").spliterator(), false)
                        .filter(item -> published.id().equals(item.path("id").asText())).findFirst().orElseThrow()
                : null;
        assertThat(guestNote).isNotNull();
        assertThat(guestNote.path("liked").asBoolean()).isFalse();
        assertThat(guestNote.path("favorited").asBoolean()).isFalse();
        assertThat(guestNote.path("likeCount").asLong()).isEqualTo(1L);
        assertThat(noteQueryService.liked("user-a", 1, 20).items())
                .singleElement()
                .extracting(NoteBO::id, NoteBO::liked, NoteBO::favorited)
                .containsExactly(published.id(), true, true);
        assertThat(noteQueryService.favorited("user-a", 1, 20).items())
                .extracting(NoteBO::id).containsExactly(published.id());

        var comment = noteCommentService.create("user-a", published.id(),
                new CreateNoteCommentDTO("comment-1", null, "第一条评论"));
        assertThat(noteCommentService.create("user-a", published.id(),
                new CreateNoteCommentDTO("comment-1", null, "第一条评论")).id()).isEqualTo(comment.id());
        var reply = noteCommentService.create("user-b", published.id(),
                new CreateNoteCommentDTO("comment-2", comment.id(), "一级回复"));
        assertThat(noteCommentService.page("user-a", published.id(), 1, 20).total()).isEqualTo(2);
        assertThatThrownBy(() -> noteCommentService.create("user-a", published.id(),
                new CreateNoteCommentDTO("comment-3", reply.id(), "禁止二级嵌套")))
                .isInstanceOf(BusinessException.class).hasMessageContaining("一级");
        assertThatThrownBy(() -> noteCommentService.delete("user-a", comment.id(), 0))
                .isInstanceOf(BusinessException.class).hasMessageContaining("已有回复");
        noteCommentService.delete("user-b", reply.id(), 0);
        noteCommentService.delete("user-a", comment.id(), 0);

        noteEngagementService.unlike("user-a", published.id());
        noteEngagementService.unlike("user-a", published.id());
        noteEngagementService.unfavorite("user-a", published.id());
        assertThat(noteEngagementService.get("user-a", published.id()))
                .extracting("likeCount", "favoriteCount", "commentCount", "liked", "favorited")
                .containsExactly(0L, 0L, 0L, false, false);
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
            assertThat(count("SELECT COUNT(*) FROM course_entitlement WHERE source_id = 'group-1'"))
                    .isEqualTo(2);
            assertThat(count("SELECT COUNT(*) FROM message_inbox WHERE event_id = 'group-event-1'"))
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
            assertThat(count("SELECT COUNT(*) FROM progress_event_inbox WHERE event_id = 'progress-event-1'"))
                    .isEqualTo(1);
            assertThat(count("SELECT COUNT(*) FROM watched_segment")).isEqualTo(9);
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
        assertThat(redisTemplate.hasKey("kw:learning:progress:snapshot:user-a:video-java-1:1")).isTrue();

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

    @Test
    void courseProgressUsesCurrentVersionAndDatabaseAndMyCoursesDeduplicateEntitlements() {
        var progressEvent = event("course-completed", 500, 1, 90_000, List.of(new PlayedRangeEventDTO(0, 90_000)));
        transactionService.process(progressEvent);
        transactionService.process(progressEvent);
        jdbcTemplate.update("""
                INSERT INTO course_entitlement(id,user_id,course_id,source_type,source_id,status,effective_at,created_at,updated_at)
                VALUES('second-source','user-a','course-java','TEST','second-source','ACTIVE',UTC_TIMESTAMP(3),UTC_TIMESTAMP(3),UTC_TIMESTAMP(3))
                """);
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
        var detail = learnerGet("/api/learning/courses/course-java/progress", "user-a");
        assertThat(detail.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode data = detail.getBody().path("data");
        assertThat(data.path("totalVideos").asInt()).isEqualTo(1);
        assertThat(data.path("completedVideos").asInt()).isEqualTo(1);
        assertThat(data.path("completionRate").asInt()).isEqualTo(100);
        assertThat(data.path("resumePositionMs").asLong()).isEqualTo(90_000);
        var mine = learnerGet("/api/learning/my-courses?pageNo=1&pageSize=1", "user-a");
        assertThat(mine.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(mine.getBody().path("data").path("total").asInt()).isEqualTo(1);
        assertThat(mine.getBody().path("data").path("items").size()).isEqualTo(1);
        assertThat(learnerGet("/api/learning/my-courses?pageNo=2&pageSize=1", "user-a").getBody()
                .path("data").path("items").size()).isZero();
        jdbcTemplate.update("""
                INSERT INTO chapter(id,course_id,title,sort_order,video_id,video_url,video_duration_ms,video_version,status,version,created_at,updated_at)
                SELECT 'chapter-java-2',course_id,'Chapter 2',2,'video-java-2',video_url,video_duration_ms,1,status,0,created_at,updated_at
                FROM chapter WHERE id='chapter-java-1'
                """);
        var partial = learnerGet("/api/learning/courses/course-java/progress", "user-a").getBody().path("data");
        assertThat(partial.path("totalVideos").asInt()).isEqualTo(2);
        assertThat(partial.path("completedVideos").asInt()).isEqualTo(1);
        assertThat(partial.path("completionRate").asInt()).isEqualTo(50);
        jdbcTemplate.update("UPDATE chapter SET video_version=2 WHERE id='chapter-java-1'");
        var changed = learnerGet("/api/learning/courses/course-java/progress", "user-a").getBody().path("data");
        assertThat(changed.path("completedVideos").asInt()).isZero();
        assertThat(changed.path("videoVersion").asInt()).isEqualTo(2);
        assertThat(changed.path("resumePositionMs").asLong()).isZero();
    }

    @Test
    void courseProgressRejectsForeignUsersExpiredEntitlementsAndUnpublishedCourses() {
        String path = "/api/learning/courses/course-java/progress";
        assertThat(learnerGet(path, "user-b").getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(learnerGet("/api/learning/my-courses", "user-b").getBody().path("data").path("total").asInt()).isZero();
        assertThat(restTemplate.getForEntity(path, JsonNode.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(learnerGet("/api/learning/my-courses?pageSize=101", "user-a").getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        jdbcTemplate.update("UPDATE course_entitlement SET expires_at=UTC_TIMESTAMP(3)-INTERVAL 1 DAY");
        assertThat(learnerGet(path, "user-a").getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(learnerGet("/api/learning/my-courses", "user-a").getBody().path("data").path("total").asInt()).isZero();
        jdbcTemplate.update("UPDATE course_entitlement SET expires_at=NULL");
        jdbcTemplate.update("UPDATE course SET status='DRAFT'");
        assertThat(learnerGet(path, "user-a").getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(learnerGet("/api/learning/my-courses", "user-a").getBody().path("data").path("total").asInt()).isZero();
    }

    @Test
    void newLearnerStartsAtFirstVideoAndUnpublishedVideosDoNotCount() {
        var before = learnerGet("/api/learning/courses/course-java/progress", "user-a").getBody().path("data");
        assertThat(before.path("videoId").asText()).isEqualTo("video-java-1");
        assertThat(before.path("lastLearnedAt").isNull()).isTrue();
        assertThat(before.path("completionRate").asInt()).isZero();
        jdbcTemplate.update("UPDATE chapter SET status='DRAFT'");
        var empty = learnerGet("/api/learning/courses/course-java/progress", "user-a").getBody().path("data");
        assertThat(empty.path("totalVideos").asInt()).isZero();
        assertThat(empty.path("completionRate").asInt()).isZero();
        assertThat(empty.path("videoId").isNull()).isTrue();
        var api = restTemplate.getForObject("/v3/api-docs", JsonNode.class);
        assertThat(api.path("paths").has("/api/learning/my-courses")).isTrue();
        assertThat(api.path("paths").has("/api/learning/courses/{courseId}/progress")).isTrue();
    }

    @Test
    void concurrentPlaybackSessionsInstallHighestEpochAndOnlyLatestSessionIsValid() throws Exception {
        var executor = Executors.newFixedThreadPool(8);
        List<Future<PlaybackSessionBO>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < 32; i++) {
                futures.add(executor.submit(() -> sessionService.start("user-a", "video-java-1")));
            }
            List<PlaybackSessionBO> sessions = new ArrayList<>();
            for (var future : futures) {
                sessions.add(future.get(10, TimeUnit.SECONDS));
            }
            var newest = sessions.stream().max(Comparator.comparingLong(PlaybackSessionBO::sessionEpoch)).orElseThrow();
            String sessionKey = "kw:learning:progress:session:v2:{user-a:video-java-1}";
            String epochKey = "kw:learning:progress:session-epoch:v2:{user-a:video-java-1}";
            assertThat(redisTemplate.opsForValue().get(epochKey)).isEqualTo(String.valueOf(newest.sessionEpoch()));
            assertThat(redisTemplate.opsForHash().get(sessionKey, "sessionId")).isEqualTo(newest.sessionId());
            assertThat(redisTemplate.getExpire(sessionKey)).isBetween(7100L, 7200L);
            assertThat(redisTemplate.getExpire(epochKey)).isBetween(2591900L, 2592000L);
            for (var session : sessions) {
                if (session != newest) {
                    assertThatThrownBy(() -> sessionService.validate("user-a", "video-java-1",
                            session.sessionId(), session.sessionEpoch(), session.videoVersion()))
                            .isInstanceOf(BusinessException.class);
                }
            }
            sessionService.validate("user-a", "video-java-1", newest.sessionId(), newest.sessionEpoch(), newest.videoVersion());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void unroutableProgressIsRejectedThenSameEventCanRetryAfterBindingIsRestored() {
        var session = sessionService.start("user-a", "video-java-1");
        var report = report("unroutable-event", session, 1, 25000, ProgressEventType.PAUSE, List.of());
        var binding = new Binding(LearningRabbitConfiguration.PROGRESS_QUEUE, Binding.DestinationType.QUEUE,
                LearningRabbitConfiguration.EVENT_EXCHANGE, LearningRabbitConfiguration.PROGRESS_ROUTING_KEY, null);
        rabbitAdmin.removeBinding(binding);
        try {
            assertThatThrownBy(() -> progressService.report("user-a", "video-java-1", report))
                    .isInstanceOf(BusinessException.class);
            assertThat(progressQueryService.get("user-a", "video-java-1").resumePositionMs()).isZero();
        } finally {
            rabbitAdmin.declareBinding(binding);
        }
        assertThat(progressService.report("user-a", "video-java-1", report).accepted()).isTrue();
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(count("SELECT COUNT(*) FROM progress_event_inbox WHERE event_id='unroutable-event'")).isEqualTo(1));
    }

    @Test
    void brokenRedisSnapshotAndEmptyRecentIndexRecoverFromMysql() {
        transactionService.process(event("recovery-event", 300, 1, 45000, List.of()));
        String key = "kw:learning:progress:snapshot:user-a:video-java-1:1";
        redisTemplate.opsForHash().put(key, "sequence", "invalid-number");
        var result = learnerGet("/api/learning/videos/video-java-1/progress", "user-a");
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().path("data").path("resumePositionMs").asLong()).isEqualTo(45000);
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
        var recent = learnerGet("/api/learning/progress/recent", "user-a");
        assertThat(recent.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(recent.getBody().path("data").size()).isEqualTo(1);
        assertThat(recent.getBody().path("data").get(0).path("resumePositionMs").asLong()).isEqualTo(45000);
    }

    private ResponseEntity<JsonNode> learnerGet(String path, String userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken(userId, "LEARNER"));
        return restTemplate.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);
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

    @Test
    void legacyQueueWithoutDeadLetterArgumentsDoesNotConflictWithCurrentDeclaration() {
        rabbitAdmin.declareQueue(new Queue("learning.group-formed", true));
        rabbitAdmin.initialize();
        assertThat(rabbitAdmin.getQueueInfo("learning.group-formed")).isNotNull();
        assertThat(rabbitAdmin.getQueueInfo(LearningRabbitConfiguration.GROUP_FORMED_QUEUE)).isNotNull();
        assertThat(LearningRabbitConfiguration.GROUP_FORMED_QUEUE).isEqualTo("learning.group-formed.v2");
    }

    private void insertCourseAndChapter() {
        jdbcTemplate.update("""
                INSERT INTO course
                  (id, category_id, title, summary, price_cents, status, version, created_at, updated_at)
                VALUES ('course-java', 'backend', 'Java', 'Java course', 9900, 'PUBLISHED', 0,
                        UTC_TIMESTAMP(3), UTC_TIMESTAMP(3))
                """);
        jdbcTemplate.update("""
                INSERT INTO chapter
                  (id, course_id, title, sort_order, video_id, video_url, video_duration_ms,
                   video_version, status, version, created_at, updated_at)
                VALUES ('chapter-java-1', 'course-java', 'Chapter 1', 1, 'video-java-1',
                        'https://example.invalid/video.m3u8', 100000, 1, 'PUBLISHED', 0,
                        UTC_TIMESTAMP(3), UTC_TIMESTAMP(3))
                """);
    }

    private void insertEntitlement(String userId) {
        jdbcTemplate.update("""
                INSERT INTO course_entitlement
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
