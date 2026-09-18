package com.knowledge.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledge.agent.mention.config.AgentRabbitConfiguration;
import com.knowledge.agent.chat.dto.CreateConversationDTO;
import com.knowledge.agent.chat.dto.SendAgentMessageDTO;
import com.knowledge.agent.chat.service.AgentConversationService;
import com.knowledge.api.learning.dto.AgentMentionedEventDTO;
import java.time.Duration;
import java.time.Instant;
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
@SpringBootTest(properties = "spring.cloud.nacos.discovery.enabled=false")
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
    AgentConversationService conversationService;

    @BeforeEach
    void prepare() {
        rabbitAdmin.purgeQueue(AgentRabbitConfiguration.AGENT_MENTION_QUEUE, true);
        rabbitAdmin.purgeQueue(AgentRabbitConfiguration.AGENT_MENTION_DEAD_QUEUE, true);
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
            rabbitTemplate.convertAndSend(AgentRabbitConfiguration.EVENT_EXCHANGE,
                    AgentRabbitConfiguration.AGENT_MENTION_ROUTING_KEY, payload);
        }

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertThat(count("SELECT COUNT(*) FROM agent_event_inbox")).isEqualTo(1);
            assertThat(count("SELECT COUNT(*) FROM agent_run")).isEqualTo(1);
        });
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM agent_run", String.class)).isEqualTo("PENDING");
        assertThat(jdbcTemplate.queryForObject("SELECT source_comment_id FROM agent_run", String.class))
                .isEqualTo("comment-1");
    }

    @Test
    void conversationSerializesRunsAndKeepsClientRequestIdempotent() {
        var conversation = conversationService.create("user-1", new CreateConversationDTO("Redis 答疑"));
        var first = conversationService.start("user-1", conversation.id(),
                new SendAgentMessageDTO("11111111-1111-1111-1111-111111111111", "什么是缓存穿透？", null));
        assertThat(conversationService.start("user-1", conversation.id(),
                new SendAgentMessageDTO("11111111-1111-1111-1111-111111111111", "什么是缓存穿透？", null)))
                .extracting("runId", "replay").containsExactly(first.runId(), true);
        assertThatThrownBy(() -> conversationService.start("user-1", conversation.id(),
                new SendAgentMessageDTO("22222222-2222-2222-2222-222222222222", "什么是缓存击穿？", null)))
                .isInstanceOf(com.knowledge.common.exception.BusinessException.class)
                .hasMessageContaining("进行中");

        conversationService.complete(first.runId(), "缓存穿透是请求不存在的数据。");
        var second = conversationService.start("user-1", conversation.id(),
                new SendAgentMessageDTO("22222222-2222-2222-2222-222222222222", "什么是缓存击穿？", null));
        assertThat(second.replay()).isFalse();
        assertThat(count("SELECT COUNT(*) FROM agent_message")).isEqualTo(3);
    }

    private int count(String sql) {
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }
}
