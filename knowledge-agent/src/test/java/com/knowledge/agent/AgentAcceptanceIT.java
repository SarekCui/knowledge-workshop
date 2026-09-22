package com.knowledge.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledge.agent.mention.config.MessagingConfiguration;
import com.knowledge.agent.mention.dao.mapper.AgentRunMapper;
import com.knowledge.agent.mention.service.ExecutionOutboxService;
import com.knowledge.agent.mention.service.RunRecoveryService;
import com.knowledge.agent.mention.service.RetryService;
import com.knowledge.agent.conversation.dto.CreateConversationDTO;
import com.knowledge.agent.conversation.dto.SendMessageDTO;
import com.knowledge.agent.conversation.service.ConversationService;
import com.knowledge.api.learning.dto.AgentMentionedEventDTO;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Clock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "spring.cloud.nacos.discovery.enabled=false",
        "knowledge.agent.execution.outbox-dispatch-delay=1h"
})
class AgentAcceptanceIT {

    private static final String JWT_SECRET = "local-test-secret-at-least-32-bytes-long";

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("knowledge_agent");

    @Container
    static final RabbitMQContainer RABBIT = new RabbitMQContainer("rabbitmq:4.1-management-alpine");

    @DynamicPropertySource
    static void infrastructure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.rabbitmq.host", RABBIT::getHost);
        registry.add("spring.rabbitmq.port", RABBIT::getAmqpPort);
        registry.add("spring.rabbitmq.username", RABBIT::getAdminUsername);
        registry.add("spring.rabbitmq.password", RABBIT::getAdminPassword);
        registry.add("knowledge.security.jwt.secret", () -> JWT_SECRET);
    }

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    RabbitAdmin rabbitAdmin;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    ConversationService conversationService;

    @Autowired
    AgentRunMapper runMapper;

    @Autowired
    ExecutionOutboxService executionOutboxService;

    @Autowired
    RunRecoveryService recoveryService;

    @Autowired
    RetryService retryService;

    @BeforeEach
    void prepare() {
        rabbitAdmin.purgeQueue(MessagingConfiguration.AGENT_MENTION_QUEUE, true);
        rabbitAdmin.purgeQueue(MessagingConfiguration.AGENT_MENTION_DEAD_QUEUE, true);
        rabbitAdmin.purgeQueue(MessagingConfiguration.AGENT_RUN_EXECUTION_QUEUE, true);
        rabbitAdmin.purgeQueue(MessagingConfiguration.AGENT_RUN_EXECUTION_DEAD_QUEUE, true);
        rabbitAdmin.purgeQueue(MessagingConfiguration.AGENT_RUN_RETRY_10S_QUEUE, true);
        rabbitAdmin.purgeQueue(MessagingConfiguration.AGENT_RUN_RETRY_60S_QUEUE, true);
        rabbitAdmin.purgeQueue(MessagingConfiguration.AGENT_RUN_RETRY_300S_QUEUE, true);
        jdbcTemplate.update("DELETE FROM agent_execution_outbox");
        jdbcTemplate.update("DELETE FROM agent_run");
        jdbcTemplate.update("DELETE FROM agent_event_inbox");
        jdbcTemplate.update("DELETE FROM agent_message");
        jdbcTemplate.update("DELETE FROM agent_conversation");
    }

    @Test
    void duplicateMentionDeliveryCreatesExactlyOnePendingCommentReplyRun() throws Exception {
        AgentMentionedEventDTO event = new AgentMentionedEventDTO("event-1", "AgentMentioned", Instant.now(),
                "comment-1", 1, "note-1", "comment-1", null, "user-1");
        String payload = objectMapper.writeValueAsString(event);
        for (int i = 0; i < 10; i++) {
            rabbitTemplate.convertAndSend(MessagingConfiguration.EVENT_EXCHANGE,
                    MessagingConfiguration.AGENT_MENTION_ROUTING_KEY, payload);
        }

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertThat(count("SELECT COUNT(*) FROM agent_event_inbox")).isEqualTo(1);
            assertThat(count("SELECT COUNT(*) FROM agent_run")).isEqualTo(1);
            assertThat(count("SELECT COUNT(*) FROM agent_execution_outbox")).isEqualTo(1);
        });
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM agent_run", String.class)).isEqualTo("PENDING");
        assertThat(jdbcTemplate.queryForObject("SELECT source_comment_id FROM agent_run", String.class))
                .isEqualTo("comment-1");
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM agent_execution_outbox", String.class))
                .isEqualTo("PENDING");
        assertThat(executionOutboxService.dispatchBatch(10)).isEqualTo(1);
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(rabbitAdmin.getQueueInfo(MessagingConfiguration.AGENT_RUN_EXECUTION_QUEUE)
                        .getMessageCount()).isEqualTo(1));
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM agent_execution_outbox", String.class))
                .isEqualTo("SENT");
    }

    @Test
    void conversationSerializesRunsAndKeepsClientRequestIdempotent() {
        var conversation = conversationService.create("user-1", new CreateConversationDTO("Redis 答疑"));
        var first = conversationService.start("user-1", conversation.id(),
                new SendMessageDTO("web:chat-send:11111111-1111-1111-1111-111111111111", "什么是缓存穿透？", null));
        assertThat(conversationService.start("user-1", conversation.id(),
                new SendMessageDTO("web:chat-send:11111111-1111-1111-1111-111111111111", "什么是缓存穿透？", null)))
                .extracting("runId", "replay").containsExactly(first.runId(), true);
        assertThatThrownBy(() -> conversationService.start("user-1", conversation.id(),
                new SendMessageDTO("web:chat-send:22222222-2222-2222-2222-222222222222", "什么是缓存击穿？", null)))
                .isInstanceOf(com.knowledge.common.exception.BusinessException.class)
                .hasMessageContaining("进行中");

        conversationService.complete(first.runId(), "缓存穿透是请求不存在的数据。");
        var second = conversationService.start("user-1", conversation.id(),
                new SendMessageDTO("web:chat-send:22222222-2222-2222-2222-222222222222", "什么是缓存击穿？", null));
        assertThat(second.replay()).isFalse();
        assertThat(count("SELECT COUNT(*) FROM agent_message")).isEqualTo(3);
    }

    @Test
    void conditionalClaimAndFencingTokenRejectASecondOrStaleWorker() throws Exception {
        AgentMentionedEventDTO event = new AgentMentionedEventDTO("event-claim", "AgentMentioned", Instant.now(),
                "comment-claim", 1, "note-claim", "comment-claim", null, "user-1");
        rabbitTemplate.convertAndSend(MessagingConfiguration.EVENT_EXCHANGE,
                MessagingConfiguration.AGENT_MENTION_ROUTING_KEY, objectMapper.writeValueAsString(event));
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(count("SELECT COUNT(*) FROM agent_run")).isEqualTo(1));

        String runId = jdbcTemplate.queryForObject("SELECT id FROM agent_run", String.class);
        LocalDateTime now = LocalDateTime.now(Clock.systemUTC());
        assertThat(runMapper.claim(runId, "worker-a", now.plusMinutes(1), now)).isEqualTo(1);
        assertThat(runMapper.claim(runId, "worker-b", now.plusMinutes(1), now)).isZero();
        long executionVersion = jdbcTemplate.queryForObject(
                "SELECT execution_version FROM agent_run WHERE id = ?", Long.class, runId);
        assertThat(runMapper.renewLease(runId, executionVersion, "worker-a", now.plusMinutes(2), now)).isEqualTo(1);
        assertThat(runMapper.renewLease(runId, executionVersion - 1, "worker-a", now.plusMinutes(2), now))
                .isZero();

        assertThat(runMapper.persistGeneratedAnswer(runId, executionVersion, "已持久化回答", now)).isEqualTo(1);
        assertThat(runMapper.markPublished(runId, executionVersion - 1, now)).isZero();
        assertThat(runMapper.markPublished(runId, executionVersion, now)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM agent_run WHERE id = ?", String.class, runId))
                .isEqualTo("SUCCEEDED");
    }

    @Test
    void expiredWorkerLeaseIsRecoveredThroughANewDurableExecutionCommand() throws Exception {
        AgentMentionedEventDTO event = new AgentMentionedEventDTO("event-recovery", "AgentMentioned", Instant.now(),
                "comment-recovery", 1, "note-recovery", "comment-recovery", null, "user-1");
        rabbitTemplate.convertAndSend(MessagingConfiguration.EVENT_EXCHANGE,
                MessagingConfiguration.AGENT_MENTION_ROUTING_KEY, objectMapper.writeValueAsString(event));
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(count("SELECT COUNT(*) FROM agent_run")).isEqualTo(1));

        String runId = jdbcTemplate.queryForObject("SELECT id FROM agent_run", String.class);
        LocalDateTime now = LocalDateTime.now(Clock.systemUTC());
        assertThat(runMapper.claim(runId, "crashed-worker", now.plusSeconds(10), now)).isEqualTo(1);
        jdbcTemplate.update("UPDATE agent_run SET lease_until = ? WHERE id = ?", now.minusSeconds(1), runId);

        assertThat(recoveryService.recoverBatch(10)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM agent_run WHERE id = ?", String.class, runId))
                .isEqualTo("PENDING");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM agent_execution_outbox WHERE run_id = ?", Integer.class, runId)).isEqualTo(2);
    }

    @Test
    void retryAtTheConfiguredAttemptLimitMarksTheRunDeadWithoutAnotherCommand() throws Exception {
        AgentMentionedEventDTO event = new AgentMentionedEventDTO("event-dead", "AgentMentioned", Instant.now(),
                "comment-dead", 1, "note-dead", "comment-dead", null, "user-1");
        rabbitTemplate.convertAndSend(MessagingConfiguration.EVENT_EXCHANGE,
                MessagingConfiguration.AGENT_MENTION_ROUTING_KEY, objectMapper.writeValueAsString(event));
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(count("SELECT COUNT(*) FROM agent_run")).isEqualTo(1));

        String runId = jdbcTemplate.queryForObject("SELECT id FROM agent_run", String.class);
        LocalDateTime now = LocalDateTime.now(Clock.systemUTC());
        assertThat(runMapper.claim(runId, "worker-a", now.plusMinutes(1), now)).isEqualTo(1);
        long executionVersion = jdbcTemplate.queryForObject(
                "SELECT execution_version FROM agent_run WHERE id = ?", Long.class, runId);

        retryService.schedule(runId, executionVersion, "COMMENT_REPLY", 5,
                "DEPENDENCY_FAILURE", "model unavailable");

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM agent_run WHERE id = ?", String.class, runId))
                .isEqualTo("DEAD");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM agent_execution_outbox WHERE run_id = ?", Integer.class, runId)).isEqualTo(1);
    }

    @Test
    void delayedRetryRouteReturnsTheDurableCommandToExecutionQueue() {
        executionOutboxService.record("retry-run", 0L, "COMMENT_REPLY",
                MessagingConfiguration.AGENT_RUN_RETRY_10S_ROUTING_KEY, LocalDateTime.now(Clock.systemUTC()));
        executionOutboxService.dispatchBatch(10);
        await().atMost(Duration.ofSeconds(3)).untilAsserted(() ->
                assertThat(jdbcTemplate.queryForObject("SELECT status FROM agent_execution_outbox", String.class))
                        .isEqualTo("SENT"));
        await().atMost(Duration.ofSeconds(3)).untilAsserted(() ->
                assertThat(rabbitAdmin.getQueueInfo(MessagingConfiguration.AGENT_RUN_RETRY_10S_QUEUE)
                        .getMessageCount()).isEqualTo(1));
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(rabbitAdmin.getQueueInfo(MessagingConfiguration.AGENT_RUN_EXECUTION_QUEUE)
                        .getMessageCount()).isEqualTo(1));
    }

    private int count(String sql) {
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }
}
