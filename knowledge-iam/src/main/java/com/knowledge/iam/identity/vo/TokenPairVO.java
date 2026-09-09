package com.knowledge.iam.identity.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "访问令牌与刷新令牌")
public record TokenPairVO(
        @Schema(description = "JWT 访问令牌") String accessToken,
        @Schema(description = "认证类型", example = "Bearer") String tokenType,
        @Schema(description = "访问令牌有效期，单位秒", example = "1800") long expiresIn,
        @Schema(description = "不透明刷新令牌，仅在签发时返回") String refreshToken,
        @Schema(description = "刷新令牌有效期，单位秒", example = "2592000") long refreshExpiresIn) {
}
