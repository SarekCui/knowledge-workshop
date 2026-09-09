package com.knowledge.marketing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.knowledge.api.common.ErrorCode;
import com.knowledge.common.exception.BusinessException;
import com.knowledge.marketing.groupbuy.bo.TradeOrderBO;
import com.knowledge.marketing.groupbuy.bo.JoinGroupBO;
import com.knowledge.marketing.groupbuy.dto.PaymentDTO;
import com.knowledge.marketing.groupbuy.service.GroupPurchaseService;
import com.knowledge.marketing.groupbuy.service.PaymentSettlementService;
import com.knowledge.marketing.notification.config.MarketingRabbitConfiguration;
import com.knowledge.marketing.notification.dao.model.NotificationTaskDO;
import com.knowledge.marketing.notification.enums.NotificationStatus;
import com.knowledge.marketing.notification.dao.mapper.NotificationTaskMapper;
import com.knowledge.marketing.notification.service.NotificationTaskDispatcher;
import java.time.LocalDateTime;
import java.time.Instant;
import java.time.ZoneOffset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.cloud.nacos.discovery.enabled=false",
        "knowledge.marketing.notification.fixed-delay=3600000",
        "knowledge.marketing.reservation.fixed-delay=3600000"
})
class MarketingAcceptanceIT {

    private static final String JWT_SECRET = "local-test-secret-at-least-32-bytes-long";

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("knowledge_marketing");

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
    GroupPurchaseService purchaseService;

    @Autowired
    PaymentSettlementService settlementService;

    @Autowired
    NotificationTaskDispatcher taskDispatcher;

    @Autowired
    NotificationTaskMapper taskMapper;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    RabbitAdmin rabbitAdmin;

    @Autowired
    TestRestTemplate restTemplate;

    private ExecutorService executor;

    @BeforeEach
    void prepare() {
        executor = Executors.newFixedThreadPool(100);
        jdbcTemplate.update("DELETE FROM notification_task");
        jdbcTemplate.update("DELETE FROM trade_order");
        jdbcTemplate.update("DELETE FROM group_participant");
        jdbcTemplate.update("DELETE FROM group_order");
        jdbcTemplate.update("DELETE FROM group_activity");
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
        rabbitAdmin.purgeQueue(MarketingRabbitConfiguration.GROUP_FORMED_QUEUE, true);
        insertActivityAndGroup(10);
    }

    @AfterEach
    void shutdownExecutor() {
        executor.shutdownNow();
    }

    @Test
    void oneHundredConcurrentRequestsCompeteForTenSlotsWithoutOverselling() throws Exception {
        CountDownLatch ready = new CountDownLatch(100);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<TradeOrderBO>> futures = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            int index = i;
            futures.add(executor.submit(() -> {
                ready.countDown();
                start.await();
                return purchaseService.join(new JoinGroupBO(
                        "group-acceptance", "user-" + index));
            }));
        }
        ready.await();
        start.countDown();

        List<TradeOrderBO> accepted = successfulResults(futures);
        assertThat(accepted).hasSize(10);
        for (int i = 0; i < accepted.size(); i++) {
            String orderId = accepted.get(i).orderId();
            String userId = jdbcTemplate.queryForObject(
                    "SELECT user_id FROM trade_order WHERE id = ?", String.class, orderId);
            settlementService.settle(orderId, "payment-" + i, userId);
        }

        assertThat(count("SELECT COUNT(*) FROM group_participant WHERE status = 'CONFIRMED'"))
                .isEqualTo(10);
        assertThat(count("SELECT confirmed_count FROM group_order WHERE id = 'group-acceptance'"))
                .isEqualTo(10);
        assertThat(count("SELECT COUNT(*) FROM trade_order"))
                .isEqualTo(10);
        assertThat(redisTemplate.opsForValue().get("kw:marketing:group:{group-acceptance}:occupied"))
                .isEqualTo("10");
    }

    @Test
    void duplicateRequestIsIdempotentAndDatabaseFailureReleasesRedisSlot() {
        JoinGroupBO request = new JoinGroupBO(
                "group-acceptance", "same-user");
        TradeOrderBO first = purchaseService.join(request);
        TradeOrderBO second = purchaseService.join(request);

        assertThat(second.orderId()).isEqualTo(first.orderId());
        assertThat(count("SELECT COUNT(*) FROM trade_order WHERE group_id = 'group-acceptance' AND user_id = 'same-user'"))
                .isEqualTo(1);

        jdbcTemplate.update("""
                INSERT INTO group_participant
                  (id, activity_id, group_id, user_id, status, created_at, updated_at)
                VALUES (?, 'activity-acceptance', 'group-acceptance', 'blocked-user',
                        'RELEASED', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3))
                """, UUID.randomUUID().toString());

        assertThatThrownBy(() -> purchaseService.join(new JoinGroupBO(
                "group-acceptance", "blocked-user")))
                .isInstanceOf(RuntimeException.class);
        assertThat(redisTemplate.opsForValue().get("kw:marketing:group:{group-acceptance}:occupied"))
                .isEqualTo("1");
        assertThat(count("SELECT COUNT(*) FROM trade_order WHERE group_id = 'group-acceptance' AND user_id = 'blocked-user'"))
                .isZero();
    }

    @Test
    void failedNotificationIsRetriedAndEventuallyMarkedSent() {
        NotificationTaskDO task = notificationTask();
        taskMapper.insert(task);
        rabbitAdmin.deleteExchange(MarketingRabbitConfiguration.EVENT_EXCHANGE);

        assertThat(taskDispatcher.dispatchBatch(10)).isZero();
        NotificationTaskDO retry = taskMapper.selectById(task.getId());
        assertThat(retry.getStatus()).isEqualTo(NotificationStatus.RETRY);
        assertThat(retry.getRetryCount()).isEqualTo(1);

        DirectExchange exchange = new DirectExchange(MarketingRabbitConfiguration.EVENT_EXCHANGE, true, false);
        Queue queue = new Queue(MarketingRabbitConfiguration.GROUP_FORMED_QUEUE, true);
        rabbitAdmin.declareExchange(exchange);
        rabbitAdmin.declareBinding(BindingBuilder.bind(queue).to(exchange)
                .with(MarketingRabbitConfiguration.GROUP_FORMED_ROUTING_KEY));
        retry.setNextRetryAt(LocalDateTime.now(ZoneOffset.UTC).minusSeconds(1));
        taskMapper.updateById(retry);

        assertThat(taskDispatcher.dispatchBatch(10)).isEqualTo(1);
        assertThat(taskMapper.selectById(task.getId()).getStatus()).isEqualTo(NotificationStatus.SENT);
    }

    @Test
    void openApiContractDocumentsMarketingOperationsAndErrors() {
        JsonNode document = restTemplate.getForObject("/v3/api-docs", JsonNode.class);

        assertThat(document.path("info").path("title").asText())
                .isEqualTo("Knowledge Workshop Marketing API");
        JsonNode join = document.path("paths").path("/api/marketing/groups/{groupId}/join").path("post");
        assertThat(join.isMissingNode()).isFalse();
        assertThat(join.path("responses").has("200")).isTrue();
        assertThat(join.path("parameters").toString()).contains("X-Request-Id");
        assertThat(document.path("paths").has("/api/marketing/orders/{orderId}/pay")).isTrue();
        assertThat(document.path("paths").has("/api/marketing/activities")).isTrue();
        assertThat(document.path("paths").has("/api/marketing/activities/{activityId}/groups")).isTrue();
        assertThat(document.path("paths").has("/api/marketing/orders")).isTrue();
        assertThat(document.path("paths").has("/api/marketing/admin/activities")).isTrue();
        assertThat(document.path("paths").has("/api/marketing/admin/notification-tasks/{taskId}/retry"))
                .isTrue();
    }

    @Test
    void userCanBrowseGroupBuyingAndOnlyReadOwnedOrder() {
        TradeOrderBO order = purchaseService.join(new JoinGroupBO("group-acceptance", "order-owner"));

        ResponseEntity<JsonNode> activities = restTemplate.exchange(
                "/api/marketing/activities", HttpMethod.GET,
                new HttpEntity<>(bearerHeaders("order-owner", "LEARNER")), JsonNode.class);
        ResponseEntity<JsonNode> groups = restTemplate.exchange(
                "/api/marketing/activities/activity-acceptance/groups", HttpMethod.GET,
                new HttpEntity<>(bearerHeaders("order-owner", "LEARNER")), JsonNode.class);
        ResponseEntity<JsonNode> owned = restTemplate.exchange(
                "/api/marketing/orders/" + order.orderId(), HttpMethod.GET,
                new HttpEntity<>(bearerHeaders("order-owner", "LEARNER")), JsonNode.class);
        ResponseEntity<JsonNode> foreign = restTemplate.exchange(
                "/api/marketing/orders/" + order.orderId(), HttpMethod.GET,
                new HttpEntity<>(bearerHeaders("other-user", "LEARNER")), JsonNode.class);

        assertThat(activities.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(activities.getBody().path("data").path("total").asLong()).isEqualTo(1);
        assertThat(groups.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(groups.getBody().path("data").path("items").get(0).path("id").asText())
                .isEqualTo("group-acceptance");
        assertThat(owned.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(owned.getBody().path("data").path("orderId").asText()).isEqualTo(order.orderId());
        assertThat(foreign.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void adminCanManageActivityAndCreateGroupIdempotently() {
        Instant startTime = Instant.now().minusSeconds(60);
        Instant endTime = Instant.now().plusSeconds(3600);
        HttpHeaders headers = bearerHeaders("admin-user", "ADMIN");
        Map<String, Object> createActivity = Map.of(
                "activityId", "activity-admin",
                "courseId", "course-admin",
                "startTime", startTime.toString(),
                "endTime", endTime.toString(),
                "targetCount", 3,
                "maxJoinPerUser", 1,
                "priceCents", 6900);

        ResponseEntity<JsonNode> created = post("/api/marketing/admin/activities", createActivity, headers);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(created.getBody().path("data").path("status").asText()).isEqualTo("DRAFT");

        Map<String, Object> updateActivity = Map.of(
                "courseId", "course-admin-updated",
                "startTime", startTime.toString(),
                "endTime", endTime.toString(),
                "targetCount", 4,
                "maxJoinPerUser", 1,
                "priceCents", 5900,
                "version", 0);
        ResponseEntity<JsonNode> updated = put(
                "/api/marketing/admin/activities/activity-admin", updateActivity, headers);
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody().path("data").path("version").asInt()).isEqualTo(1);

        ResponseEntity<JsonNode> activated = patch(
                "/api/marketing/admin/activities/activity-admin/status",
                Map.of("status", "ACTIVE", "version", 1), headers);
        assertThat(activated.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map<String, Object> createGroup = Map.of(
                "groupId", "group-admin",
                "ownerUserId", "group-owner",
                "expiresAt", Instant.now().plusSeconds(1800).toString());
        ResponseEntity<JsonNode> first = post(
                "/api/marketing/admin/activities/activity-admin/groups", createGroup, headers);
        ResponseEntity<JsonNode> replay = post(
                "/api/marketing/admin/activities/activity-admin/groups", createGroup, headers);

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(replay.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(replay.getBody().path("data").path("id").asText()).isEqualTo("group-admin");
        assertThat(count("SELECT COUNT(*) FROM group_order WHERE id = 'group-admin'")).isEqualTo(1);
    }

    @Test
    void learnerCannotUseAdminApiAndAdminCanScheduleFailedNotificationForRetry() {
        ResponseEntity<JsonNode> forbidden = restTemplate.exchange(
                "/api/marketing/admin/activities", HttpMethod.GET,
                new HttpEntity<>(bearerHeaders("learner-user", "LEARNER")), JsonNode.class);
        assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        NotificationTaskDO task = notificationTask();
        task.setStatus(NotificationStatus.RETRY);
        task.setLastError("broker: 消息代理不可用");
        taskMapper.insert(task);
        ResponseEntity<JsonNode> retried = post(
                "/api/marketing/admin/notification-tasks/" + task.getId() + "/retry",
                null, bearerHeaders("admin-user", "ADMIN"));

        assertThat(retried.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(retried.getBody().path("data").path("status").asText()).isEqualTo("PENDING");
        assertThat(taskMapper.selectById(task.getId()).getLastError()).isNull();
    }

    @Test
    void missingOrderUsesUnifiedErrorAndCorrelatedRequestId() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken("user-acceptance"));
        ResponseEntity<JsonNode> response = restTemplate.exchange(
                "/api/marketing/orders/missing-order/pay", HttpMethod.POST,
                new HttpEntity<>(new PaymentDTO("payment-missing"), headers), JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getHeaders().getContentType()).isNotNull();
        assertThat(response.getHeaders().getContentType().isCompatibleWith(MediaType.APPLICATION_JSON)).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().path("code").asInt()).isEqualTo(404);
        assertThat(response.getBody().path("message").asText()).isEqualTo("订单不存在");
        assertThat(response.getBody().path("data").isNull()).isTrue();
        assertThat(response.getBody().path("requestId").asText())
                .isEqualTo(response.getHeaders().getFirst("X-Request-Id"));
    }

    @Test
    void protectedApiRejectsMissingToken() {
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                "/api/marketing/groups/group-acceptance/join",
                null,
                JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().path("code").asInt()).isEqualTo(401);
        assertThat(response.getBody().path("requestId").asText())
                .isEqualTo(response.getHeaders().getFirst("X-Request-Id"));
    }

    @Test
    void authenticatedJoinUsesJwtSubjectAsUserIdentity() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken("jwt-user"));
        ResponseEntity<JsonNode> response = restTemplate.exchange(
                "/api/marketing/groups/group-acceptance/join", HttpMethod.POST,
                new HttpEntity<>(headers),
                JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT user_id FROM trade_order WHERE group_id = 'group-acceptance' AND user_id = 'jwt-user'",
                String.class))
                .isEqualTo("jwt-user");
    }

    @Test
    void anotherUserCannotPayOwnedOrder() {
        TradeOrderBO order = purchaseService.join(new JoinGroupBO(
                "group-acceptance", "owner-user"));

        assertThatThrownBy(() -> settlementService.settle(order.orderId(), "foreign-payment", "other-user"))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    private HttpHeaders bearerHeaders(String userId, String... roles) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken(userId, roles));
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private ResponseEntity<JsonNode> post(String path, Object body, HttpHeaders headers) {
        return restTemplate.exchange(path, HttpMethod.POST, new HttpEntity<>(body, headers), JsonNode.class);
    }

    private ResponseEntity<JsonNode> put(String path, Object body, HttpHeaders headers) {
        return restTemplate.exchange(path, HttpMethod.PUT, new HttpEntity<>(body, headers), JsonNode.class);
    }

    private ResponseEntity<JsonNode> patch(String path, Object body, HttpHeaders headers) {
        return restTemplate.exchange(path, HttpMethod.PATCH, new HttpEntity<>(body, headers), JsonNode.class);
    }

    private String accessToken(String userId) {
        return accessToken(userId, "LEARNER");
    }

    private String accessToken(String userId, String... roles) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("knowledge-iam")
                .subject(userId)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .claim("username", "acceptance-user")
                .claim("roles", List.of(roles))
                .build();
        SecretKeySpec key = new SecretKeySpec(JWT_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        return encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    private List<TradeOrderBO> successfulResults(List<Future<TradeOrderBO>> futures) throws Exception {
        List<TradeOrderBO> result = new ArrayList<>();
        for (Future<TradeOrderBO> future : futures) {
            try {
                result.add(future.get());
            } catch (ExecutionException expectedRejection) {
                assertThat(expectedRejection.getCause())
                        .isInstanceOfSatisfying(BusinessException.class,
                                exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONFLICT));
            }
        }
        return result;
    }

    private void insertActivityAndGroup(int capacity) {
        jdbcTemplate.update("""
                INSERT INTO group_activity
                  (id, course_id, status, start_time, end_time, target_count, max_join_per_user,
                   price_cents, version, created_at, updated_at)
                VALUES ('activity-acceptance', 'course-java', 'ACTIVE', UTC_TIMESTAMP(3) - INTERVAL 1 DAY,
                        UTC_TIMESTAMP(3) + INTERVAL 1 DAY, ?, 1, 9900, 0, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3))
                """, capacity);
        jdbcTemplate.update("""
                INSERT INTO group_order
                  (id, activity_id, owner_user_id, status, target_count, confirmed_count,
                   expires_at, version, created_at, updated_at)
                VALUES ('group-acceptance', 'activity-acceptance', 'owner', 'FORMING', ?, 0,
                        UTC_TIMESTAMP(3) + INTERVAL 1 DAY, 0, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3))
                """, capacity);
    }

    private NotificationTaskDO notificationTask() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        NotificationTaskDO task = new NotificationTaskDO();
        task.setId(UUID.randomUUID().toString());
        task.setEventId("acceptance-event-" + UUID.randomUUID());
        task.setTaskType("GROUP_FORMED");
        task.setPayload("{\"eventId\":\"acceptance\"}");
        task.setStatus(NotificationStatus.PENDING);
        task.setRetryCount(0);
        task.setNextRetryAt(now.minusSeconds(1));
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        return task;
    }

    private long count(String sql) {
        return jdbcTemplate.queryForObject(sql, Long.class);
    }
}
