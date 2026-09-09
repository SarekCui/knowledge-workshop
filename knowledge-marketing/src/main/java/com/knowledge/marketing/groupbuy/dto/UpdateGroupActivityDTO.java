package com.knowledge.marketing.groupbuy.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record UpdateGroupActivityDTO(
        @NotBlank @Size(max = 64) String courseId,
        @NotNull Instant startTime,
        @NotNull Instant endTime,
        @NotNull @Min(2) Integer targetCount,
        @NotNull @Positive Integer maxJoinPerUser,
        @NotNull @PositiveOrZero Long priceCents,
        @NotNull @PositiveOrZero Integer version) {
}
