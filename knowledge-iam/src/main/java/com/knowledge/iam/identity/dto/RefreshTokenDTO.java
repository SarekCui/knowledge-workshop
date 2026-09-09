package com.knowledge.iam.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "刷新令牌请求")
public record RefreshTokenDTO(
        @NotBlank
        @Size(max = 512)
        @Schema(description = "登录或上次刷新返回的不透明刷新令牌")
        String refreshToken) {
}
