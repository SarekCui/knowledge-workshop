package com.knowledge.agent.mention.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessagingConfiguration {

    public static final String EVENT_EXCHANGE = "knowledge.events";
    public static final String DEAD_LETTER_EXCHANGE = "knowledge.events.dlx";
    public static final String AGENT_MENTION_QUEUE = "agent.mentioned.v1";
    public static final String AGENT_MENTION_DEAD_QUEUE = "agent.mentioned.dlq";
    public static final String AGENT_MENTION_ROUTING_KEY = "learning.note.agent-mentioned.v1";
    public static final String AGENT_MENTION_DEAD_ROUTING_KEY = "learning.note.agent-mentioned.v1";
    public static final String EXECUTION_EXCHANGE = "knowledge.agent.commands";
    public static final String EXECUTION_DEAD_LETTER_EXCHANGE = "knowledge.agent.commands.dlx";
    public static final String AGENT_RUN_EXECUTION_QUEUE = "agent.run.execute.v1";
    public static final String AGENT_RUN_EXECUTION_DEAD_QUEUE = "agent.run.execute.dlq.v1";
    public static final String AGENT_RUN_REQUESTED_ROUTING_KEY = "agent.run.requested.v1";
    public static final String AGENT_RUN_RETRY_10S_QUEUE = "agent.run.execute.retry.10s.v1";
    public static final String AGENT_RUN_RETRY_60S_QUEUE = "agent.run.execute.retry.60s.v1";
    public static final String AGENT_RUN_RETRY_300S_QUEUE = "agent.run.execute.retry.300s.v1";
    public static final String AGENT_RUN_RETRY_10S_ROUTING_KEY = "agent.run.retry.10s.v1";
    public static final String AGENT_RUN_RETRY_60S_ROUTING_KEY = "agent.run.retry.60s.v1";
    public static final String AGENT_RUN_RETRY_300S_ROUTING_KEY = "agent.run.retry.300s.v1";

    @Bean
    DirectExchange agentEventExchange() {
        return new DirectExchange(EVENT_EXCHANGE, true, false);
    }

    @Bean
    DirectExchange agentDeadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    Queue agentMentionQueue() {
        return QueueBuilder.durable(AGENT_MENTION_QUEUE)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(AGENT_MENTION_DEAD_ROUTING_KEY)
                .build();
    }

    @Bean
    Queue agentMentionDeadQueue() {
        return QueueBuilder.durable(AGENT_MENTION_DEAD_QUEUE).build();
    }

    @Bean
    Binding agentMentionBinding() {
        return BindingBuilder.bind(agentMentionQueue()).to(agentEventExchange()).with(AGENT_MENTION_ROUTING_KEY);
    }

    @Bean
    Binding agentMentionDeadBinding() {
        return BindingBuilder.bind(agentMentionDeadQueue()).to(agentDeadLetterExchange())
                .with(AGENT_MENTION_DEAD_ROUTING_KEY);
    }

    @Bean
    DirectExchange agentExecutionExchange() {
        return new DirectExchange(EXECUTION_EXCHANGE, true, false);
    }

    @Bean
    DirectExchange agentExecutionDeadLetterExchange() {
        return new DirectExchange(EXECUTION_DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    Queue agentRunExecutionQueue() {
        return QueueBuilder.durable(AGENT_RUN_EXECUTION_QUEUE)
                .withArgument("x-queue-type", "quorum")
                .deadLetterExchange(EXECUTION_DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(AGENT_RUN_REQUESTED_ROUTING_KEY)
                .build();
    }

    @Bean
    Queue agentRunExecutionDeadQueue() {
        return QueueBuilder.durable(AGENT_RUN_EXECUTION_DEAD_QUEUE).build();
    }

    @Bean
    Binding agentRunExecutionBinding() {
        return BindingBuilder.bind(agentRunExecutionQueue()).to(agentExecutionExchange())
                .with(AGENT_RUN_REQUESTED_ROUTING_KEY);
    }

    @Bean
    Binding agentRunExecutionDeadBinding() {
        return BindingBuilder.bind(agentRunExecutionDeadQueue()).to(agentExecutionDeadLetterExchange())
                .with(AGENT_RUN_REQUESTED_ROUTING_KEY);
    }

    @Bean
    Queue agentRunRetry10SecondsQueue() {
        return retryQueue(AGENT_RUN_RETRY_10S_QUEUE, 10_000);
    }

    @Bean
    Queue agentRunRetry60SecondsQueue() {
        return retryQueue(AGENT_RUN_RETRY_60S_QUEUE, 60_000);
    }

    @Bean
    Queue agentRunRetry300SecondsQueue() {
        return retryQueue(AGENT_RUN_RETRY_300S_QUEUE, 300_000);
    }

    @Bean
    Binding agentRunRetry10SecondsBinding() {
        return retryBinding(agentRunRetry10SecondsQueue(), AGENT_RUN_RETRY_10S_ROUTING_KEY);
    }

    @Bean
    Binding agentRunRetry60SecondsBinding() {
        return retryBinding(agentRunRetry60SecondsQueue(), AGENT_RUN_RETRY_60S_ROUTING_KEY);
    }

    @Bean
    Binding agentRunRetry300SecondsBinding() {
        return retryBinding(agentRunRetry300SecondsQueue(), AGENT_RUN_RETRY_300S_ROUTING_KEY);
    }

    private Queue retryQueue(String name, int delayMilliseconds) {
        return QueueBuilder.durable(name)
                .ttl(delayMilliseconds)
                .deadLetterExchange(EXECUTION_EXCHANGE)
                .deadLetterRoutingKey(AGENT_RUN_REQUESTED_ROUTING_KEY)
                .build();
    }

    private Binding retryBinding(Queue queue, String routingKey) {
        return BindingBuilder.bind(queue).to(agentExecutionExchange()).with(routingKey);
    }
}
