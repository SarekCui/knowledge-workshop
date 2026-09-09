package com.knowledge.iam.identity.bo;

public record RefreshTokenBO(String userId, String token, long expiresInSeconds) {
}
