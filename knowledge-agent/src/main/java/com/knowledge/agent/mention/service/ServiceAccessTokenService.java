package com.knowledge.agent.mention.service;

import com.knowledge.api.iam.client.IamOAuthFeignClient;
import com.knowledge.api.iam.dto.OAuth2AccessTokenDTO;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;

import jakarta.annotation.Resource;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Obtains and reuses IAM-issued client-credentials tokens for Learning calls. */
@Service
public class ServiceAccessTokenService {

    private static final long REFRESH_SKEW_SECONDS = 30;
    private static final String LEARNING_SCOPES = "learning.comment.read learning.comment.write";

    @Resource
    private IamOAuthFeignClient iamOAuthFeignClient;

    @Resource
    private Clock clock;

    @Value("${knowledge.agent.m2m.client-id:knowledge-agent}")
    private String clientId;

    @Value("${knowledge.agent.m2m.client-secret}")
    private String clientSecret;

    private volatile CachedToken cachedToken;

    public String accessToken() {
        CachedToken current = cachedToken;
        if (current != null && current.isUsable(clock.instant())) {
            return current.value();
        }
        synchronized (this) {
            current = cachedToken;
            if (current != null && current.isUsable(clock.instant())) {
                return current.value();
            }
            OAuth2AccessTokenDTO response = iamOAuthFeignClient.clientCredentials(
                    basicAuthorization(), tokenRequestBody());
            if (response == null || response.accessToken() == null || response.accessToken().isBlank()
                    || response.expiresIn() <= REFRESH_SKEW_SECONDS) {
                throw new IllegalStateException("IAM returned an invalid client-credentials token response");
            }
            cachedToken = new CachedToken(response.accessToken(),
                    clock.instant().plusSeconds(response.expiresIn() - REFRESH_SKEW_SECONDS));
            return cachedToken.value();
        }
    }

    private String basicAuthorization() {
        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            throw new IllegalStateException("Agent M2M client credentials are not configured");
        }
        String credentials = clientId + ":" + clientSecret;
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    private String tokenRequestBody() {
        return "grant_type=client_credentials&scope="
                + URLEncoder.encode(LEARNING_SCOPES, StandardCharsets.UTF_8);
    }

    private record CachedToken(String value, Instant refreshAt) {
        private boolean isUsable(Instant now) {
            return now.isBefore(refreshAt);
        }
    }
}
