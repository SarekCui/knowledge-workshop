package com.knowledge.learning.course.bo;

import com.knowledge.learning.course.enums.ChapterStatus;

public record ChapterBO(
        String id,
        String courseId,
        String title,
        int sortOrder,
        String videoId,
        String videoUrl,
        long videoDurationMs,
        int videoVersion,
        ChapterStatus status,
        int version) {
}
