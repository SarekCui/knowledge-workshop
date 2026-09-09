package com.knowledge.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

class RequestIdWebFilterTest {

    private final RequestIdWebFilter filter = new RequestIdWebFilter();

    @Test
    void preservesSafeRequestIdInDownstreamRequestAndResponse() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/points")
                .header(RequestIdWebFilter.HEADER, "web-20260907:001"));
        AtomicReference<ServerWebExchange> downstream = new AtomicReference<>();
        WebFilterChain chain = captured -> {
            downstream.set(captured);
            captured.getResponse().getHeaders().add(RequestIdWebFilter.HEADER, "downstream-value");
            return captured.getResponse().setComplete();
        };

        filter.filter(exchange, chain).block();

        assertThat(downstream.get().getRequest().getHeaders().getFirst(RequestIdWebFilter.HEADER))
                .isEqualTo("web-20260907:001");
        assertThat(exchange.getResponse().getHeaders().getFirst(RequestIdWebFilter.HEADER))
                .isEqualTo("web-20260907:001");
        assertThat(exchange.getResponse().getHeaders().get(RequestIdWebFilter.HEADER)).hasSize(1);
    }

    @Test
    void replacesUnsafeRequestId() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/points")
                .header(RequestIdWebFilter.HEADER, "unsafe request id"));
        AtomicReference<ServerWebExchange> downstream = new AtomicReference<>();

        filter.filter(exchange, captured -> {
            downstream.set(captured);
            captured.getResponse().getHeaders().add(RequestIdWebFilter.HEADER, "downstream-value");
            return captured.getResponse().setComplete();
        }).block();

        String generated = downstream.get().getRequest().getHeaders().getFirst(RequestIdWebFilter.HEADER);
        assertThat(generated).matches("[0-9a-f-]{36}");
        assertThat(exchange.getResponse().getHeaders().getFirst(RequestIdWebFilter.HEADER)).isEqualTo(generated);
        assertThat(exchange.getResponse().getHeaders().get(RequestIdWebFilter.HEADER)).hasSize(1);
    }
}
