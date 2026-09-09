package com.knowledge.learning.course.dto;

import com.knowledge.learning.course.enums.ChapterStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CreateChapterDTO(
        @NotBlank @Size(max = 100) String title,
        @NotNull @PositiveOrZero Integer sortOrder,
        @NotBlank @Size(max = 64) String videoId,
        @NotBlank @Size(max = 500) String videoUrl,
        @NotNull @Positive Long videoDurationMs,
        @NotNull @Positive Integer videoVersion,
        @NotNull ChapterStatus status) {
}
