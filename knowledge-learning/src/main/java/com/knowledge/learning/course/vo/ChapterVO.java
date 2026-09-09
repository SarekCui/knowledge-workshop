package com.knowledge.learning.course.vo;

public record ChapterVO(
        String id,
        String courseId,
        String title,
        int sortOrder,
        String videoId,
        String videoUrl,
        long videoDurationMs,
        int videoVersion,
        String status,
        int version) {
}
