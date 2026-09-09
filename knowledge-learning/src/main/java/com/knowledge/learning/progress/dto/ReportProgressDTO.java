package com.knowledge.learning.progress.dto;

import com.knowledge.learning.progress.enums.ProgressEventType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ReportProgressDTO(
        @NotBlank @Size(max = 128) String eventId,
        @NotBlank @Size(max = 64) String sessionId,
        @Positive long sessionEpoch,
        @Positive long sequence,
        @NotNull ProgressEventType eventType,
        @PositiveOrZero long positionMs,
        @NotNull @Size(max = 20) List<@Valid PlayedRangeDTO> playedRanges,
        @NotNull Instant clientOccurredAt,
        @NotNull @DecimalMin("0.25") @DecimalMax("4.0") BigDecimal playbackRate) {
}
