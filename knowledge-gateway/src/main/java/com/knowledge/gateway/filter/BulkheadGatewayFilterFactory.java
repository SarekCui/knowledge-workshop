package com.knowledge.gateway.filter;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.reactor.bulkhead.operator.BulkheadOperator;
import java.time.Duration;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;

@Component
public class BulkheadGatewayFilterFactory
        extends AbstractGatewayFilterFactory<BulkheadGatewayFilterFactory.Config> {

    public BulkheadGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        BulkheadConfig bulkheadConfig = BulkheadConfig.custom()
                .maxConcurrentCalls(config.getMaxConcurrentCalls())
                .maxWaitDuration(Duration.ZERO)
                .build();
        Bulkhead bulkhead = Bulkhead.of(config.getName(), bulkheadConfig);
        return (exchange, chain) -> chain.filter(exchange)
                .transformDeferred(BulkheadOperator.of(bulkhead));
    }

    public static class Config {

        private String name;
        private int maxConcurrentCalls = 100;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getMaxConcurrentCalls() {
            return maxConcurrentCalls;
        }

        public void setMaxConcurrentCalls(int maxConcurrentCalls) {
            this.maxConcurrentCalls = maxConcurrentCalls;
        }
    }
}
