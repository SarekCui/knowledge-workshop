package com.knowledge.learning.entitlement.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LearningRabbitConfiguration {

    public static final String EVENT_EXCHANGE = "knowledge.events";
    public static final String DEAD_LETTER_EXCHANGE = "knowledge.events.dlx";
    public static final String GROUP_FORMED_QUEUE = "learning.group-formed.v2";
    public static final String GROUP_FORMED_DEAD_QUEUE = "learning.group-formed.dlq";
    public static final String GROUP_FORMED_ROUTING_KEY = "marketing.group.formed.v1";
    public static final String PROGRESS_QUEUE = "learning.video-progress";
    public static final String PROGRESS_DEAD_QUEUE = "learning.video-progress.dlq";
    public static final String PROGRESS_ROUTING_KEY = "learning.video.progress-reported.v1";
    public static final String AGENT_MENTION_QUEUE = "agent.mentioned.v1";
    public static final String AGENT_MENTION_DEAD_QUEUE = "agent.mentioned.dlq";
    public static final String AGENT_MENTION_ROUTING_KEY = "learning.note.agent-mentioned.v1";

    @Bean
    DirectExchange learningEventExchange() {
        return new DirectExchange(EVENT_EXCHANGE, true, false);
    }

    @Bean
    DirectExchange learningDeadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    Queue learningGroupFormedQueue() {
        return durableQueue(GROUP_FORMED_QUEUE, GROUP_FORMED_ROUTING_KEY);
    }

    @Bean
    Binding learningGroupFormedBinding(
            @Qualifier("learningEventExchange") DirectExchange learningEventExchange,
            @Qualifier("learningGroupFormedQueue") Queue learningGroupFormedQueue) {
        return BindingBuilder.bind(learningGroupFormedQueue).to(learningEventExchange).with(GROUP_FORMED_ROUTING_KEY);
    }

    @Bean
    Queue learningProgressQueue() {
        return durableQueue(PROGRESS_QUEUE, PROGRESS_ROUTING_KEY);
    }

    @Bean
    Binding learningProgressBinding(
            @Qualifier("learningEventExchange") DirectExchange learningEventExchange,
            @Qualifier("learningProgressQueue") Queue learningProgressQueue) {
        return BindingBuilder.bind(learningProgressQueue).to(learningEventExchange).with(PROGRESS_ROUTING_KEY);
    }

    @Bean
    Queue agentMentionQueue() {
        return durableQueue(AGENT_MENTION_QUEUE, AGENT_MENTION_ROUTING_KEY);
    }

    @Bean
    Binding agentMentionBinding(
            @Qualifier("learningEventExchange") DirectExchange learningEventExchange,
            @Qualifier("agentMentionQueue") Queue agentMentionQueue) {
        return BindingBuilder.bind(agentMentionQueue).to(learningEventExchange).with(AGENT_MENTION_ROUTING_KEY);
    }

    @Bean
    Queue learningGroupFormedDeadQueue() {
        return QueueBuilder.durable(GROUP_FORMED_DEAD_QUEUE).build();
    }

    @Bean
    Binding learningGroupFormedDeadBinding(
            @Qualifier("learningDeadLetterExchange") DirectExchange learningDeadLetterExchange,
            @Qualifier("learningGroupFormedDeadQueue") Queue learningGroupFormedDeadQueue) {
        return BindingBuilder.bind(learningGroupFormedDeadQueue)
                .to(learningDeadLetterExchange).with(GROUP_FORMED_ROUTING_KEY);
    }

    @Bean
    Queue learningProgressDeadQueue() {
        return QueueBuilder.durable(PROGRESS_DEAD_QUEUE).build();
    }

    @Bean
    Binding learningProgressDeadBinding(
            @Qualifier("learningDeadLetterExchange") DirectExchange learningDeadLetterExchange,
            @Qualifier("learningProgressDeadQueue") Queue learningProgressDeadQueue) {
        return BindingBuilder.bind(learningProgressDeadQueue)
                .to(learningDeadLetterExchange).with(PROGRESS_ROUTING_KEY);
    }

    @Bean
    Queue agentMentionDeadQueue() {
        return QueueBuilder.durable(AGENT_MENTION_DEAD_QUEUE).build();
    }

    @Bean
    Binding agentMentionDeadBinding(
            @Qualifier("learningDeadLetterExchange") DirectExchange learningDeadLetterExchange,
            @Qualifier("agentMentionDeadQueue") Queue agentMentionDeadQueue) {
        return BindingBuilder.bind(agentMentionDeadQueue)
                .to(learningDeadLetterExchange).with(AGENT_MENTION_ROUTING_KEY);
    }

    private Queue durableQueue(String queueName, String routingKey) {
        return QueueBuilder.durable(queueName)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(routingKey)
                .build();
    }
}
