package com.knowledge.iam.profile.bo;

public record UserProfileBO(
        String userId,
        String username,
        String nickname,
        String avatarUrl,
        String bio,
        int version) {
}
