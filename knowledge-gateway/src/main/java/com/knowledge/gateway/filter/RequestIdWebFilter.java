package com.knowledge.gateway.filter;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class RequestIdWebFilter implements WebFilter, Ordered {

    public static final String HEADER = "X-Request-Id";
    private static final Pattern SAFE_REQUEST_ID = Pattern.compile("[A-Za-z0-9._:-]{1,64}");
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestIdWebFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String requestId = resolveRequestId(exchange.getRequest().getHeaders().getFirst(HEADER));
        long startNanos = System.nanoTime();
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> headers.set(HEADER, requestId))
                .build();
        ServerWebExchange correlatedExchange = exchange.mutate().request(request).build();
        correlatedExchange.getResponse().beforeCommit(() -> {
            correlatedExchange.getResponse().getHeaders().set(HEADER, requestId);
            return Mono.empty();
        });
        return chain.filter(correlatedExchange).doFinally(signalType -> {
            HttpStatusCode status = correlatedExchange.getResponse().getStatusCode();
            LOGGER.atInfo()
                    .addKeyValue("event", "http_request")
                    .addKeyValue("requestId", requestId)
                    .addKeyValue("method", request.getMethod().name())
                    .addKeyValue("path", request.getPath().value())
                    .addKeyValue("status", status == null ? 0 : status.value())
                    .addKeyValue("durationMs", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos))
                    .addKeyValue("signal", signalType.name())
                    .log("HTTP request completed");
        });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private String resolveRequestId(String candidate) {
        if (candidate != null && SAFE_REQUEST_ID.matcher(candidate).matches()) {
            return candidate;
        }
        return UUID.randomUUID().toString();
    }
}
