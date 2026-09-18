package com.knowledge.agent.mention.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AgentRabbitConfiguration {

    public static final String EVENT_EXCHANGE = "knowledge.events";
    public static final String DEAD_LETTER_EXCHANGE = "knowledge.events.dlx";
    public static final String AGENT_MENTION_QUEUE = "agent.mentioned.v1";
    public static final String AGENT_MENTION_DEAD_QUEUE = "agent.mentioned.dlq";
    public static final String AGENT_MENTION_ROUTING_KEY = "learning.note.agent-mentioned.v1";
    public static final String AGENT_MENTION_DEAD_ROUTING_KEY = "learning.note.agent-mentioned.v1";

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
}
