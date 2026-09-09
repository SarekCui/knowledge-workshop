package com.knowledge.learning.course.vo;

import java.time.LocalDateTime;

public record CourseVO(
        String id,
        String title,
        String summary,
        String coverUrl,
        long priceCents,
        String status,
        int version,
        LocalDateTime updatedAt) {
}
