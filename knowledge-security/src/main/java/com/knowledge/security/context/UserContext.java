package com.knowledge.security.context;

import java.util.List;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * 当前登录用户上下文，只读取 Spring Security 已认证的请求身份。
 */
public final class UserContext {

    private UserContext() {
    }

    public static String getUserId() {
        return getJwt().getSubject();
    }

    public static String getUserIdOrNull() {
        return isAuthenticated() ? getJwt().getSubject() : null;
    }

    public static String getUsername() {
        return getJwt().getClaimAsString("username");
    }

    public static List<String> getRoles() {
        List<String> roles = getJwt().getClaimAsStringList("roles");
        return roles == null ? List.of() : List.copyOf(roles);
    }

    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof Jwt;
    }

    private static Jwt getJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new AuthenticationCredentialsNotFoundException("用户未登录");
        }
        return jwt;
    }
}
