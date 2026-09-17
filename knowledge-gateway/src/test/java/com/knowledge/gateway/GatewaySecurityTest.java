package com.knowledge.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(properties = {
        "knowledge.security.jwt.secret=local-test-secret-at-least-32-bytes-long",
        "spring.cloud.nacos.discovery.enabled=false"
})
@AutoConfigureWebTestClient
class GatewaySecurityTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void rejectsMissingTokenWithUnifiedResultAndRequestId() {
        webTestClient.get()
                .uri("/api/points/leaderboard?season=2026-Q3&limit=10")
                .header("X-Request-Id", "gateway-auth-test")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().valueEquals("X-Request-Id", "gateway-auth-test")
                .expectBody()
                .jsonPath("$.code").isEqualTo(401)
                .jsonPath("$.requestId").isEqualTo("gateway-auth-test");
    }

    @Test
    void replacesUnsafeRequestIdOnInvalidBearerToken() {
        webTestClient.get()
                .uri("/api/points/leaderboard?season=2026-Q3&limit=10")
                .header("X-Request-Id", "unsafe/request/id")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().value("X-Request-Id", value -> assertThat(value).matches("[0-9a-f-]{36}"))
                .expectBody()
                .jsonPath("$.code").isEqualTo(401)
                .jsonPath("$.requestId").value(value -> assertThat(value.toString()).matches("[0-9a-f-]{36}"));
    }

    @Test
    void publicDiscoveryRoutesPassAuthenticationBoundaryWithoutToken() {
        for (String path : new String[]{
                "/api/learning/notes/public?pageNo=1&pageSize=20",
                "/api/learning/catalog/categories",
                "/api/iam/users/public?userIds=user-demo"}) {
            webTestClient.get().uri(path).exchange()
                    .expectStatus().value(status -> assertThat(status).isNotEqualTo(401));
        }
    }

    @Test
    void rejectsDirectAccessToInternalFallback() {
        webTestClient.get()
                .uri("/internal/gateway/fallback/points")
                .header("X-Request-Id", "gateway-fallback-test")
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().valueEquals("X-Request-Id", "gateway-fallback-test")
                .expectBody()
                .jsonPath("$.code").isEqualTo(404)
                .jsonPath("$.message").isEqualTo("资源不存在")
                .jsonPath("$.requestId").isEqualTo("gateway-fallback-test");
    }
}
