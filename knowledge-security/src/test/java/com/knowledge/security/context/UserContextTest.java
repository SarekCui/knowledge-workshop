package com.knowledge.security.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class UserContextTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReadAuthenticatedUserFromSpringSecurityContext() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("user-1001")
                .claim("username", "sarek")
                .claim("roles", List.of("USER", "CREATOR"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(new JwtAuthenticationToken(
                jwt, List.of(new SimpleGrantedAuthority("ROLE_USER")), "sarek"));
        SecurityContextHolder.setContext(securityContext);

        assertThat(UserContext.isAuthenticated()).isTrue();
        assertThat(UserContext.getUserId()).isEqualTo("user-1001");
        assertThat(UserContext.getUsername()).isEqualTo("sarek");
        assertThat(UserContext.getRoles()).containsExactly("USER", "CREATOR");
    }

    @Test
    void shouldRejectMissingAuthentication() {
        assertThat(UserContext.isAuthenticated()).isFalse();
        assertThatThrownBy(UserContext::getUserId)
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class)
                .hasMessage("用户未登录");
    }
}
