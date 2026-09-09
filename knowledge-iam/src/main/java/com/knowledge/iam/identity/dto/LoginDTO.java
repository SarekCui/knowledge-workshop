package com.knowledge.iam.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "账号密码登录参数")
public record LoginDTO(
        @Schema(description = "登录名", example = "demo")
        @NotBlank @Size(max = 64) String username,
        @Schema(description = "密码", example = "Knowledge@123", format = "password")
        @NotBlank @Size(min = 8, max = 72) String password) {
}
