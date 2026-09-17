package com.knowledge.marketing.notification.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MarketingRabbitConfiguration {

    public static final String EVENT_EXCHANGE = "knowledge.events";
    public static final String DEAD_LETTER_EXCHANGE = "knowledge.events.dlx";
    public static final String GROUP_FORMED_QUEUE = "learning.group-formed.v2";
    public static final String GROUP_FORMED_DEAD_QUEUE = "learning.group-formed.dlq";
    public static final String GROUP_FORMED_ROUTING_KEY = "marketing.group.formed.v1";

    @Bean
    DirectExchange knowledgeEventExchange() {
        return new DirectExchange(EVENT_EXCHANGE, true, false);
    }

    @Bean
    Queue groupFormedQueue() {
        return QueueBuilder.durable(GROUP_FORMED_QUEUE)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(GROUP_FORMED_ROUTING_KEY)
                .build();
    }

    @Bean
    Binding groupFormedBinding(
            @Qualifier("knowledgeEventExchange") DirectExchange knowledgeEventExchange,
            @Qualifier("groupFormedQueue") Queue groupFormedQueue) {
        return BindingBuilder.bind(groupFormedQueue).to(knowledgeEventExchange).with(GROUP_FORMED_ROUTING_KEY);
    }

    @Bean
    DirectExchange knowledgeDeadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    Queue groupFormedDeadQueue() {
        return QueueBuilder.durable(GROUP_FORMED_DEAD_QUEUE).build();
    }

    @Bean
    Binding groupFormedDeadBinding(
            @Qualifier("knowledgeDeadLetterExchange") DirectExchange knowledgeDeadLetterExchange,
            @Qualifier("groupFormedDeadQueue") Queue groupFormedDeadQueue) {
        return BindingBuilder.bind(groupFormedDeadQueue)
                .to(knowledgeDeadLetterExchange).with(GROUP_FORMED_ROUTING_KEY);
    }
}
