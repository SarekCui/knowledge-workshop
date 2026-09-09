package com.knowledge.learning.progress.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record PlayedRangeDTO(
        @PositiveOrZero long startMs,
        @Positive long endMs) {
}
