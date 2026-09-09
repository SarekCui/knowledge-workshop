package com.knowledge.iam.identity.bo;

public record TokenPairBO(
        String accessToken,
        long accessTokenExpiresInSeconds,
        String refreshToken,
        long refreshTokenExpiresInSeconds) {
}
