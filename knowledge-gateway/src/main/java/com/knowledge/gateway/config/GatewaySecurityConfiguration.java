package com.knowledge.gateway.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledge.api.common.ErrorCode;
import com.knowledge.api.common.Result;
import com.knowledge.gateway.filter.RequestIdWebFilter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfiguration {

    @Bean
    SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            ReactiveJwtDecoder jwtDecoder,
            ObjectMapper objectMapper) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(Customizer.withDefaults())
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .authorizeExchange(authorize -> authorize
                        .pathMatchers(HttpMethod.OPTIONS).permitAll()
                        .pathMatchers(
                                "/api/iam/auth/login",
                                "/api/iam/auth/refresh",
                                "/api/iam/auth/logout",
                                "/actuator/health",
                                "/actuator/info")
                        .permitAll()
                        .pathMatchers("/api/**").authenticated()
                        .anyExchange().permitAll())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((exchange, exception) ->
                                writeError(exchange, ErrorCode.UNAUTHORIZED, "未认证或访问令牌无效", objectMapper))
                        .accessDeniedHandler((exchange, exception) ->
                                writeError(exchange, ErrorCode.FORBIDDEN, "无权限访问该资源", objectMapper)))
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtDecoder(jwtDecoder)
                                .jwtAuthenticationConverter(this::convertAuthentication))
                        .authenticationEntryPoint((exchange, exception) ->
                                writeError(exchange, ErrorCode.UNAUTHORIZED, "未认证或访问令牌无效", objectMapper)))
                .build();
    }

    @Bean
    ReactiveJwtDecoder jwtDecoder(
            @Value("${knowledge.security.jwt.secret}") String secret,
            @Value("${knowledge.security.jwt.issuer:knowledge-iam}") String issuer) {
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalArgumentException("JWT_SECRET must contain at least 32 bytes");
        }
        SecretKey secretKey = new SecretKeySpec(secretBytes, "HmacSHA256");
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withSecretKey(secretKey).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
        return decoder;
    }

    private Mono<JwtAuthenticationToken> convertAuthentication(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        List<SimpleGrantedAuthority> authorities = (roles == null ? List.<String>of() : roles).stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
        return Mono.just(new JwtAuthenticationToken(jwt, authorities, jwt.getClaimAsString("username")));
    }

    private Mono<Void> writeError(
            ServerWebExchange exchange, ErrorCode errorCode, String message, ObjectMapper objectMapper) {
        String requestId = exchange.getRequest().getHeaders().getFirst(RequestIdWebFilter.HEADER);
        Result<Void> result = Result.fail(errorCode, message, requestId == null ? "unknown" : requestId);
        byte[] body;
        try {
            body = objectMapper.writeValueAsBytes(result);
        } catch (JsonProcessingException exception) {
            body = "{\"code\":500,\"message\":\"服务器内部错误\"}".getBytes(StandardCharsets.UTF_8);
        }
        exchange.getResponse().setStatusCode(errorCode.getHttpStatus());
        exchange.getResponse().getHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
