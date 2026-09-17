package com.knowledge.iam;

import static org.assertj.core.api.Assertions.assertThat;

import com.knowledge.iam.identity.dto.LoginDTO;
import com.knowledge.iam.identity.dto.RefreshTokenDTO;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IamAcceptanceIT {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("knowledge_iam")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.cloud.nacos.discovery.enabled", () -> "false");
        registry.add("knowledge.security.jwt.secret", () -> "local-test-secret-at-least-32-bytes-long");
        registry.add("knowledge.storage.enabled", () -> "false");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void seedUser() {
        jdbcTemplate.update("DELETE FROM refresh_token");
        jdbcTemplate.update("DELETE FROM user_profile");
        jdbcTemplate.update("DELETE FROM user_role");
        jdbcTemplate.update("DELETE FROM access_role");
        jdbcTemplate.update("DELETE FROM user_account");
        jdbcTemplate.update("INSERT INTO user_account "
                        + "(id, username, password_hash, status, created_at, updated_at) "
                        + "VALUES (?, ?, ?, 'ENABLED', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3))",
                "user-demo", "demo", passwordEncoder.encode("Knowledge@123"));
        jdbcTemplate.update("INSERT INTO user_profile "
                        + "(user_id, nickname, avatar_object_key, bio, version, created_at, updated_at) "
                        + "VALUES (?, ?, NULL, NULL, 0, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3))",
                "user-demo", "demo");
        jdbcTemplate.update("INSERT INTO access_role "
                        + "(id, code, name, status, created_at, updated_at) "
                        + "VALUES ('role-learner', 'LEARNER', '学习者', 'ENABLED', UTC_TIMESTAMP(3), UTC_TIMESTAMP(3))");
        jdbcTemplate.update("INSERT INTO user_role (id, user_id, role_id, created_at) "
                + "VALUES ('user-role-demo', 'user-demo', 'role-learner', UTC_TIMESTAMP(3))");
    }

    @Test
    void logsInWithBcryptPasswordAndReturnsJwt() {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/iam/auth/login",
                new LoginDTO("demo", "Knowledge@123"), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<?, ?> data = (Map<?, ?>) response.getBody().get("data");
        assertThat(data.get("tokenType")).isEqualTo("Bearer");
        assertThat(data.get("expiresIn")).isEqualTo(1800);
        assertThat(data.get("accessToken").toString().split("\\.")).hasSize(3);
        assertThat(data.get("refreshToken").toString()).isNotBlank();
        assertThat(data.get("refreshExpiresIn")).isEqualTo(2592000);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM refresh_token WHERE token_hash = ?",
                Integer.class, data.get("refreshToken"))).isZero();
    }

    @Test
    void browserCookieRotatesAndLogoutPreventsRecoveryWithoutExposingRefreshToken() {
        ResponseEntity<Map> login = webRequest("login", new LoginDTO("demo", "Knowledge@123"), null);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<?, ?> data = (Map<?, ?>) login.getBody().get("data");
        assertThat(data.containsKey("refreshToken")).isFalse();
        String originalCookie = login.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(originalCookie).contains("HttpOnly", "Secure", "SameSite=Strict", "Path=/api/iam/web/auth");
        assertThat(login.getHeaders().getCacheControl()).isEqualTo("no-store");
        ResponseEntity<Map> refreshed = webRequest("refresh", Map.of(), originalCookie.split(";", 2)[0]);
        assertThat(refreshed.getStatusCode()).isEqualTo(HttpStatus.OK);
        String nextCookie = refreshed.getHeaders().getFirst(HttpHeaders.SET_COOKIE).split(";", 2)[0];
        assertThat(nextCookie).isNotEqualTo(originalCookie.split(";", 2)[0]);
        ResponseEntity<Map> logout = webRequest("logout", Map.of(), nextCookie);
        assertThat(logout.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(logout.getHeaders().getFirst(HttpHeaders.SET_COOKIE)).contains("Max-Age=0");
        assertThat(webRequest("refresh", Map.of(), nextCookie).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void browserAuthenticationRejectsUntrustedOriginAndMissingDedicatedHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.setOrigin("https://untrusted.example");
        headers.set("X-Web-Auth", "1");
        ResponseEntity<Map> untrusted = restTemplate.exchange("http://localhost:" + port + "/api/iam/web/auth/login",
                HttpMethod.POST, new HttpEntity<>(new LoginDTO("demo", "Knowledge@123"), headers), Map.class);
        assertThat(untrusted.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        headers.setOrigin("http://127.0.0.1:5173");
        headers.remove("X-Web-Auth");
        ResponseEntity<Map> noHeader = restTemplate.exchange("http://localhost:" + port + "/api/iam/web/auth/refresh",
                HttpMethod.POST, new HttpEntity<>(Map.of(), headers), Map.class);
        assertThat(noHeader.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private ResponseEntity<Map> webRequest(String action, Object body, String cookie) {
        HttpHeaders headers = new HttpHeaders();
        headers.setOrigin("http://127.0.0.1:5173");
        headers.set("X-Web-Auth", "1");
        if (cookie != null) headers.set(HttpHeaders.COOKIE, cookie);
        return restTemplate.exchange("http://localhost:" + port + "/api/iam/web/auth/" + action,
                HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
    }

    @Test
    void rotatesRefreshTokenAndRejectsReusedTokenFamily() {
        Map<?, ?> loginData = loginData();
        String originalRefreshToken = loginData.get("refreshToken").toString();

        ResponseEntity<Map> refreshed = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/iam/auth/refresh",
                new RefreshTokenDTO(originalRefreshToken), Map.class);
        assertThat(refreshed.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<?, ?> refreshedData = (Map<?, ?>) refreshed.getBody().get("data");
        String rotatedRefreshToken = refreshedData.get("refreshToken").toString();
        assertThat(rotatedRefreshToken).isNotEqualTo(originalRefreshToken);

        ResponseEntity<Map> reuse = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/iam/auth/refresh",
                new RefreshTokenDTO(originalRefreshToken), Map.class);
        assertThat(reuse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        ResponseEntity<Map> familyRevoked = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/iam/auth/refresh",
                new RefreshTokenDTO(rotatedRefreshToken), Map.class);
        assertThat(familyRevoked.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void logoutRevokesRefreshSessionAndIsIdempotent() {
        String refreshToken = loginData().get("refreshToken").toString();

        ResponseEntity<Map> firstLogout = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/iam/auth/logout",
                new RefreshTokenDTO(refreshToken), Map.class);
        ResponseEntity<Map> repeatedLogout = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/iam/auth/logout",
                new RefreshTokenDTO(refreshToken), Map.class);

        assertThat(firstLogout.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(repeatedLogout.getStatusCode()).isEqualTo(HttpStatus.OK);
        ResponseEntity<Map> refresh = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/iam/auth/refresh",
                new RefreshTokenDTO(refreshToken), Map.class);
        assertThat(refresh.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void returnsSameUnauthorizedResponseForWrongPassword() {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/iam/auth/login",
                new LoginDTO("demo", "WrongPass123"), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().get("code")).isEqualTo(401);
        assertThat(response.getBody().get("message")).isEqualTo("用户名或密码错误");
    }

    @Test
    void exposesLoginOpenApiContract() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/v3/api-docs", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .contains("/api/iam/auth/login")
                .contains("/api/iam/auth/refresh")
                .contains("/api/iam/auth/logout")
                .contains("LoginDTO")
                .contains("RefreshTokenDTO")
                .contains("TokenPairVO");
    }

    @Test
    void readsUpdatesAndPublishesCurrentUserProfile() {
        String accessToken = loginData().get("accessToken").toString();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        ResponseEntity<Map> initial = restTemplate.exchange("http://localhost:" + port + "/api/iam/profile",
                HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        assertThat(initial.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Map<?, ?>) initial.getBody().get("data")).get("nickname")).isEqualTo("demo");

        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> update = Map.of("nickname", "海边学习者", "bio", "持续学习", "version", 0);
        ResponseEntity<Map> updated = restTemplate.exchange("http://localhost:" + port + "/api/iam/profile",
                HttpMethod.PUT, new HttpEntity<>(update, headers), Map.class);
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<?, ?> profile = (Map<?, ?>) updated.getBody().get("data");
        assertThat(profile.get("nickname")).isEqualTo("海边学习者");
        assertThat(profile.get("bio")).isEqualTo("持续学习");
        assertThat(profile.get("version")).isEqualTo(1);

        ResponseEntity<Map> stale = restTemplate.exchange("http://localhost:" + port + "/api/iam/profile",
                HttpMethod.PUT, new HttpEntity<>(update, headers), Map.class);
        assertThat(stale.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        ResponseEntity<Map> published = restTemplate.exchange(
                "http://localhost:" + port + "/api/iam/users/public?userIds=user-demo",
                HttpMethod.GET, HttpEntity.EMPTY, Map.class);
        assertThat(published.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Map<?, ?>) ((java.util.List<?>) published.getBody().get("data")).get(0)).get("nickname"))
                .isEqualTo("海边学习者");
    }

    private Map<?, ?> loginData() {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/iam/auth/login",
                new LoginDTO("demo", "Knowledge@123"), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return (Map<?, ?>) response.getBody().get("data");
    }
}
