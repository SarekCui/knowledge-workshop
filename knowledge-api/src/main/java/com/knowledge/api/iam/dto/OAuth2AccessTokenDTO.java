package com.knowledge.api.iam.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * OAuth 2.0 client-credentials token response shared by an internal client and IAM.
 *
 * <p>The JSON property names follow RFC 6749 and therefore intentionally do not use the
 * project's usual camel-case API convention.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OAuth2AccessTokenDTO(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") long expiresIn,
        String scope) {
}
