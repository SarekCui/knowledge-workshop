package com.knowledge.iam.identity.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.knowledge.iam.identity.bo.AccessTokenBO;
import com.knowledge.iam.identity.bo.AuthenticatedUserBO;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jwt.JWTParser;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

class JwtTokenServiceTest {

    @Test
    void issuesExpectedIdentityAndExpiryClaims() throws Exception {
        Instant now = Instant.parse("2026-09-08T00:00:00Z");
        SecretKey key = new SecretKeySpec(
                "local-test-secret-at-least-32-bytes-long".getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        JwtTokenService service = new JwtTokenService(
                new NimbusJwtEncoder(new ImmutableSecret<>(key)),
                Clock.fixed(now, ZoneOffset.UTC),
                "knowledge-iam",
                Duration.ofMinutes(30));

        AccessTokenBO token = service.issue(
                new AuthenticatedUserBO("user-001", "demo", List.of("LEARNER")));
        var claims = JWTParser.parse(token.accessToken()).getJWTClaimsSet();

        assertThat(claims.getSubject()).isEqualTo("user-001");
        assertThat(claims.getIssuer()).isEqualTo("knowledge-iam");
        assertThat(claims.getStringClaim("username")).isEqualTo("demo");
        assertThat(claims.getStringListClaim("roles")).containsExactly("LEARNER");
        assertThat(claims.getExpirationTime().toInstant()).isEqualTo(now.plusSeconds(1800));
        assertThat(token.expiresInSeconds()).isEqualTo(1800);
    }
}
