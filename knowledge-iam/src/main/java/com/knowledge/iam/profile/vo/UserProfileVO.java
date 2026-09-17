package com.knowledge.iam.profile.vo;

public record UserProfileVO(
        String userId,
        String username,
        String nickname,
        String avatarUrl,
        String bio,
        int version) {
}
