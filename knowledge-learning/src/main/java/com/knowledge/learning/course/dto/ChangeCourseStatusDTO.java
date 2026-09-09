package com.knowledge.learning.course.dto;

import com.knowledge.learning.course.enums.CourseStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ChangeCourseStatusDTO(
        @NotNull CourseStatus status,
        @NotNull @PositiveOrZero Integer version) {
}
