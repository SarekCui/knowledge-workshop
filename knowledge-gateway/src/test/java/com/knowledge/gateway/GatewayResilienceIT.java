package com.knowledge.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "knowledge.security.jwt.secret=local-test-secret-at-least-32-bytes-long",
                "spring.cloud.nacos.discovery.enabled=false",
                "spring.cloud.gateway.server.webflux.httpclient.response-timeout=200ms",
                "resilience4j.timelimiter.instances.iamGateway.timeoutDuration=200ms",
                "resilience4j.circuitbreaker.instances.iamGateway.waitDurationInOpenState=200ms"
        })
@AutoConfigureWebTestClient
class GatewayResilienceIT {

    private static final AtomicInteger GET_ATTEMPTS = new AtomicInteger();
    private static final AtomicInteger POST_ATTEMPTS = new AtomicInteger();
    private static final AtomicBoolean POST_HEALTHY = new AtomicBoolean();
    private static DisposableServer backend;

    @DynamicPropertySource
    static void downstream(DynamicPropertyRegistry registry) {
        backend = HttpServer.create()
                .port(0)
                .handle((request, response) -> {
                    String path = request.uri();
                    if (path.startsWith("/api/iam/auth/refresh")) {
                        return Mono.delay(Duration.ofSeconds(1))
                                .then(response.status(200).sendString(Mono.just("{\"status\":\"slow\"}")).then());
                    }
                    if ("GET".equals(request.method().name())) {
                        int attempt = GET_ATTEMPTS.incrementAndGet();
                        if (attempt == 1) {
                            return response.status(503).send().then();
                        }
                        return response.status(200).sendString(Mono.just("{\"status\":\"ok\"}")).then();
                    }
                    POST_ATTEMPTS.incrementAndGet();
                    if (POST_HEALTHY.get()) {
                        return response.status(200).sendString(Mono.just("{\"status\":\"ok\"}")).then();
                    }
                    return response.status(503).send().then();
                })
                .bindNow();
        registry.add("IAM_URI", () -> "http://localhost:" + backend.port());
    }

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @BeforeEach
    void reset() {
        GET_ATTEMPTS.set(0);
        POST_ATTEMPTS.set(0);
        POST_HEALTHY.set(false);
        circuitBreakerRegistry.circuitBreaker("iamGateway").reset();
    }

    @AfterAll
    static void stopBackend() {
        if (backend != null) {
            backend.disposeNow();
        }
    }

    @Test
    void retriesGetOnceButNeverRetriesPost() {
        webTestClient.get()
                .uri("/api/iam/auth/login")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("ok");
        assertThat(GET_ATTEMPTS).hasValue(2);

        webTestClient.post()
                .uri("/api/iam/auth/login")
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.code").isEqualTo(503);
        assertThat(POST_ATTEMPTS).hasValue(1);
    }

    @Test
    void timesOutSlowDownstreamAndReturnsUnifiedFallback() {
        webTestClient.post()
                .uri("/api/iam/auth/refresh")
                .header("X-Request-Id", "gateway-timeout-test")
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.code").isEqualTo(503)
                .jsonPath("$.requestId").isEqualTo("gateway-timeout-test");
    }

    @Test
    void opensCircuitAndRecoversThroughHalfOpenState() throws InterruptedException {
        for (int index = 0; index < 5; index++) {
            webTestClient.post()
                    .uri("/api/iam/auth/login")
                    .exchange()
                    .expectStatus().isEqualTo(503);
        }
        assertThat(circuitBreakerRegistry.circuitBreaker("iamGateway").getState().name())
                .isEqualTo("OPEN");

        POST_HEALTHY.set(true);
        int attemptsBeforeOpenRejection = POST_ATTEMPTS.get();
        webTestClient.post()
                .uri("/api/iam/auth/login")
                .exchange()
                .expectStatus().isEqualTo(503);
        assertThat(POST_ATTEMPTS).hasValue(attemptsBeforeOpenRejection);

        Thread.sleep(250);
        webTestClient.post()
                .uri("/api/iam/auth/login")
                .exchange()
                .expectStatus().isOk();
        webTestClient.post()
                .uri("/api/iam/auth/login")
                .exchange()
                .expectStatus().isOk();
        assertThat(circuitBreakerRegistry.circuitBreaker("iamGateway").getState().name())
                .isEqualTo("CLOSED");
    }
}
