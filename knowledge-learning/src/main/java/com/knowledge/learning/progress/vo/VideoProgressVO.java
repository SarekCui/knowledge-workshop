package com.knowledge.learning.progress.vo;

import java.time.LocalDateTime;

public record VideoProgressVO(
        String courseId,
        String chapterId,
        String videoId,
        int videoVersion,
        long resumePositionMs,
        long maxPositionMs,
        long durationMs,
        long watchedSeconds,
        int completionRate,
        String status,
        long sessionEpoch,
        long sequence,
        LocalDateTime updatedAt) {
}
