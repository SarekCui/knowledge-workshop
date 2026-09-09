package com.knowledge.marketing.groupbuy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record CreateGroupDTO(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9._-]{1,64}") String groupId,
        @NotBlank @Size(max = 64) String ownerUserId,
        @NotNull Instant expiresAt) {
}
