package com.knowledge.learning.course.bo;

import java.time.Instant;

public record CourseLearningBO(String courseId, String title, String coverUrl, long totalVideos,
        long completedVideos, int completionRate, String chapterId, String videoId, Integer videoVersion,
        long resumePositionMs, Instant lastLearnedAt) {
}
