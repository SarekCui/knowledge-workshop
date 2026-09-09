package com.knowledge.marketing.groupbuy.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record CreateGroupActivityDTO(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9._-]{1,64}") String activityId,
        @NotBlank @Size(max = 64) String courseId,
        @NotNull Instant startTime,
        @NotNull Instant endTime,
        @NotNull @Min(2) Integer targetCount,
        @NotNull @Positive Integer maxJoinPerUser,
        @NotNull @PositiveOrZero Long priceCents) {
}
