package com.knowledge.learning.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record UpdateCourseDTO(
        @NotBlank @Size(max = 100) String title,
        @NotNull @Size(max = 2000) String summary,
        @Size(max = 500) String coverUrl,
        @NotNull @PositiveOrZero Long priceCents,
        @NotNull @PositiveOrZero Integer version,
        @Size(max = 64) String categoryId) {

    public UpdateCourseDTO(String title, String summary, String coverUrl, Long priceCents, Integer version) {
        this(title, summary, coverUrl, priceCents, version, null);
    }
}
