package com.knowledge.security.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class ResourceServerConfigurationTest {

    private final ResourceServerConfiguration configuration = new ResourceServerConfiguration();

    @Test
    void mapsStandardScopeArrayToAuthorities() {
        assertThat(authorityNames(jwt(List.of("learning.comment.read", "learning.comment.write"))))
                .containsExactly("SCOPE_learning.comment.read", "SCOPE_learning.comment.write");
    }

    @Test
    void mapsSpaceDelimitedScopeForCompatibleTokens() {
        assertThat(authorityNames(jwt("learning.comment.read learning.comment.write")))
                .containsExactly("SCOPE_learning.comment.read", "SCOPE_learning.comment.write");
    }

    private List<String> authorityNames(Jwt jwt) {
        return configuration.authorities(jwt).stream().map(GrantedAuthority::getAuthority).toList();
    }

    private Jwt jwt(Object scope) {
        Instant now = Instant.parse("2026-09-20T00:00:00Z");
        return new Jwt("token", now, now.plusSeconds(300), Map.of("alg", "RS256"), Map.of("scope", scope));
    }
}
