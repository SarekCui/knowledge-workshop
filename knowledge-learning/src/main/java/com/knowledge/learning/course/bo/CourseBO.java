package com.knowledge.learning.course.bo;

import com.knowledge.learning.course.enums.CourseStatus;
import java.time.LocalDateTime;

public record CourseBO(
        String id,
        String title,
        String summary,
        String coverUrl,
        long priceCents,
        CourseStatus status,
        int version,
        LocalDateTime updatedAt) {
}
