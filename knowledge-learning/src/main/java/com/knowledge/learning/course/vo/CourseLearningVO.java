package com.knowledge.learning.course.vo;

import java.time.Instant;

public record CourseLearningVO(String courseId, String title, String coverUrl, long totalVideos,
        long completedVideos, int completionRate, String chapterId, String videoId, Integer videoVersion,
        long resumePositionMs, Instant lastLearnedAt) {
}
