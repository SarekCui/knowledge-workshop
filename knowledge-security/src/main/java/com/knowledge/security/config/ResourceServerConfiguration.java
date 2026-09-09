package com.knowledge.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledge.api.common.ErrorCode;
import com.knowledge.api.common.Result;
import com.knowledge.common.web.RequestIdSupport;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
public class ResourceServerConfiguration {

    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    SecurityFilterChain resourceServerSecurityFilterChain(
            HttpSecurity http, JwtDecoder jwtDecoder, ObjectMapper objectMapper) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health", "/actuator/info", "/v3/api-docs/**",
                                "/swagger-ui.html", "/swagger-ui/**")
                        .permitAll()
                        .anyRequest().authenticated())
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(formLogin -> formLogin.disable())
                .logout(logout -> logout.disable())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint((request, response, exception) ->
                                writeError(response, request.getHeader(RequestIdSupport.HEADER),
                                        ErrorCode.UNAUTHORIZED, "未认证或访问令牌无效", objectMapper))
                        .accessDeniedHandler((request, response, exception) ->
                                writeError(response, request.getHeader(RequestIdSupport.HEADER),
                                        ErrorCode.FORBIDDEN, "无权限访问该资源", objectMapper)))
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(JwtDecoder.class)
    JwtDecoder jwtDecoder(
            @Value("${knowledge.security.jwt.secret}") String secret,
            @Value("${knowledge.security.jwt.issuer:knowledge-iam}") String issuer) {
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalArgumentException("JWT_SECRET must contain at least 32 bytes");
        }
        SecretKey secretKey = new SecretKeySpec(secretBytes, "HmacSHA256");
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
        return decoder;
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setPrincipalClaimName("username");
        converter.setJwtGrantedAuthoritiesConverter(this::authorities);
        return converter;
    }

    private Collection<GrantedAuthority> authorities(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        return (roles == null ? List.<String>of() : roles).stream()
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
    }

    private void writeError(HttpServletResponse response, String requestId, ErrorCode errorCode,
                            String message, ObjectMapper objectMapper) throws IOException {
        String resolvedRequestId = RequestIdSupport.resolve(requestId);
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader(RequestIdSupport.HEADER, resolvedRequestId);
        objectMapper.writeValue(response.getOutputStream(),
                Result.fail(errorCode, message, resolvedRequestId));
    }
}
