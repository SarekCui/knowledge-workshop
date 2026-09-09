package com.knowledge.points.ranking.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PointsRabbitConfiguration {

    public static final String EVENT_EXCHANGE = "knowledge.events";
    public static final String POINT_GRANT_QUEUE = "points.grant";
    public static final String POINT_GRANT_ROUTING_KEY = "points.grant.v1";

    @Bean
    DirectExchange pointsEventExchange() {
        return new DirectExchange(EVENT_EXCHANGE, true, false);
    }

    @Bean
    Queue pointGrantQueue() {
        return new Queue(POINT_GRANT_QUEUE, true);
    }

    @Bean
    Binding pointGrantBinding(DirectExchange pointsEventExchange, Queue pointGrantQueue) {
        return BindingBuilder.bind(pointGrantQueue).to(pointsEventExchange).with(POINT_GRANT_ROUTING_KEY);
    }
}
