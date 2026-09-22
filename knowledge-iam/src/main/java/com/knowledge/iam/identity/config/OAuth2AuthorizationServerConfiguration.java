package com.knowledge.iam.identity.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.JwtGenerator;
import org.springframework.security.oauth2.server.authorization.token.DelegatingOAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2AccessTokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;

/** OAuth 2.0 authorization-server endpoints for workload-to-workload access. */
@Configuration
public class OAuth2AuthorizationServerConfiguration {

    private static final String AGENT_READ_SCOPE = "learning.comment.read";
    private static final String AGENT_WRITE_SCOPE = "learning.comment.write";

    @Bean
    @Order(1)
    SecurityFilterChain authorizationServerSecurityFilterChain(
            HttpSecurity http, OAuth2TokenGenerator<OAuth2Token> m2mTokenGenerator) throws Exception {
        OAuth2AuthorizationServerConfigurer authorizationServer =
                OAuth2AuthorizationServerConfigurer.authorizationServer();
        http.securityMatcher(authorizationServer.getEndpointsMatcher())
                .with(authorizationServer, server -> server.tokenGenerator(m2mTokenGenerator))
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                .csrf(csrf -> csrf.ignoringRequestMatchers(authorizationServer.getEndpointsMatcher()));
        return http.build();
    }

    @Bean
    RegisteredClientRepository registeredClientRepository(
            org.springframework.security.crypto.password.PasswordEncoder passwordEncoder,
            @Value("${knowledge.security.m2m.agent-client-id:knowledge-agent}") String agentClientId,
            @Value("${knowledge.security.m2m.agent-client-secret}") String agentClientSecret,
            @Value("${knowledge.security.m2m.access-token-ttl:5m}") Duration accessTokenTtl) {
        if (agentClientSecret.isBlank()) {
            throw new IllegalArgumentException("AGENT_M2M_CLIENT_SECRET must not be blank");
        }
        RegisteredClient agentClient = RegisteredClient.withId(UUID.nameUUIDFromBytes(
                        agentClientId.getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString())
                .clientId(agentClientId)
                .clientSecret(passwordEncoder.encode(agentClientSecret))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope(AGENT_READ_SCOPE)
                .scope(AGENT_WRITE_SCOPE)
                .tokenSettings(TokenSettings.builder().accessTokenTimeToLive(accessTokenTtl).build())
                .build();
        return new InMemoryRegisteredClientRepository(agentClient);
    }

    @Bean
    JWKSource<SecurityContext> m2mJwkSource(
            @Value("${knowledge.security.m2m.rsa-private-key}") String pemPrivateKey,
            @Value("${knowledge.security.m2m.key-id:iam-m2m-2026-01}") String keyId) throws Exception {
        RSAPrivateKey privateKey = readPrivateKey(pemPrivateKey);
        RSAPublicKey publicKey = publicKey(privateKey);
        RSAKey key = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .algorithm(JWSAlgorithm.RS256)
                .keyID(keyId)
                .build();
        return new ImmutableJWKSet<>(new JWKSet(key));
    }

    @Bean
    OAuth2TokenCustomizer<JwtEncodingContext> m2mTokenCustomizer(
            @Value("${knowledge.security.m2m.audience:knowledge-learning}") String audience) {
        return context -> {
            if (AuthorizationGrantType.CLIENT_CREDENTIALS.equals(context.getAuthorizationGrantType())) {
                context.getClaims().audience(java.util.List.of(audience));
                context.getClaims().claim("client_id", context.getRegisteredClient().getClientId());
            }
        };
    }

    @Bean
    OAuth2TokenGenerator<OAuth2Token> m2mTokenGenerator(
            JWKSource<SecurityContext> m2mJwkSource,
            OAuth2TokenCustomizer<JwtEncodingContext> m2mTokenCustomizer) {
        JwtGenerator jwtGenerator = new JwtGenerator(new NimbusJwtEncoder(m2mJwkSource));
        jwtGenerator.setJwtCustomizer(m2mTokenCustomizer);
        return new DelegatingOAuth2TokenGenerator(jwtGenerator, new OAuth2AccessTokenGenerator());
    }

    @Bean
    AuthorizationServerSettings authorizationServerSettings(
            @Value("${knowledge.security.m2m.issuer:http://localhost:8083}") String issuer) {
        return AuthorizationServerSettings.builder().issuer(issuer).build();
    }

    private RSAPrivateKey readPrivateKey(String value) throws Exception {
        String normalized = value.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("M2M_JWT_PRIVATE_KEY must not be blank");
        }
        byte[] encoded = Base64.getDecoder().decode(normalized);
        String decodedValue = new String(encoded, StandardCharsets.US_ASCII);
        if (decodedValue.contains("-----BEGIN PRIVATE KEY-----")) {
            String pemPayload = decodedValue.replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            encoded = Base64.getDecoder().decode(pemPayload);
        }
        return (RSAPrivateKey) KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(encoded));
    }

    private RSAPublicKey publicKey(RSAPrivateKey privateKey) throws Exception {
        if (!(privateKey instanceof RSAPrivateCrtKey crtKey)) {
            throw new IllegalArgumentException("M2M_JWT_PRIVATE_KEY must be a PKCS#8 RSA CRT private key");
        }
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(
                new RSAPublicKeySpec(crtKey.getModulus(), crtKey.getPublicExponent()));
    }
}
