package com.knowledge.learning.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CreateCourseDTO(
        @NotBlank @Size(max = 100) String title,
        @NotNull @Size(max = 2000) String summary,
        @Size(max = 500) String coverUrl,
        @NotNull @PositiveOrZero Long priceCents,
        @Size(max = 64) String categoryId) {

    public CreateCourseDTO(String title, String summary, String coverUrl, Long priceCents) {
        this(title, summary, coverUrl, priceCents, null);
    }
}
