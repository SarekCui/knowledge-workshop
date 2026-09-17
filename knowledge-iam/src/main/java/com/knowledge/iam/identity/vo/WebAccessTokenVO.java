package com.knowledge.iam.identity.vo;

import io.swagger.v3.oas.annotations.media.Schema;

public record WebAccessTokenVO(
        @Schema(description = "短期访问令牌") String accessToken,
        @Schema(description = "令牌类型") String tokenType,
        @Schema(description = "有效期，单位秒") long expiresIn) {
}
