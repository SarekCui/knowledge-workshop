package com.knowledge.iam.profile.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserProfileDTO(
        @NotBlank(message = "昵称不能为空")
        @Size(max = 32, message = "昵称最多32个字符") String nickname,
        @Size(max = 200, message = "个人简介最多200个字符") String bio,
        @Min(value = 0, message = "版本号不能小于0")
        @Max(value = Integer.MAX_VALUE, message = "版本号不合法") int version) {
}
