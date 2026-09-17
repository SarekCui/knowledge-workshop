package com.knowledge.learning.course.bo;

import java.time.LocalDateTime;

public record CourseCatalogBO(
        String id,
        String title,
        String summary,
        String coverUrl,
        long priceCents,
        String categoryId,
        String categoryName,
        long chapterCount,
        long totalDurationMs,
        boolean entitled,
        LocalDateTime updatedAt) {
}
