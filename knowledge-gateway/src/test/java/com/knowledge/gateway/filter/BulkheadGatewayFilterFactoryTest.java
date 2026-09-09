package com.knowledge.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.resilience4j.bulkhead.BulkheadFullException;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class BulkheadGatewayFilterFactoryTest {

    @Test
    void rejectsExcessConcurrentRequestWithoutWaiting() {
        BulkheadGatewayFilterFactory factory = new BulkheadGatewayFilterFactory();
        BulkheadGatewayFilterFactory.Config config = new BulkheadGatewayFilterFactory.Config();
        config.setName("testBulkhead");
        config.setMaxConcurrentCalls(1);
        GatewayFilter filter = factory.apply(config);
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/test").build());

        Disposable first = filter.filter(exchange, ignored -> Mono.never()).subscribe();
        try {
            StepVerifier.create(filter.filter(exchange, ignored -> Mono.empty()))
                    .expectErrorSatisfies(error -> assertThat(error)
                            .isInstanceOf(BulkheadFullException.class))
                    .verify(Duration.ofSeconds(1));
        } finally {
            first.dispose();
        }
    }
}
