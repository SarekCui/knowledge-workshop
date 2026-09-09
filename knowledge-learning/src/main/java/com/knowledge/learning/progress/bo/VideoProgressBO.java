package com.knowledge.learning.progress.bo;

import com.knowledge.learning.progress.enums.ProgressStatus;
import java.time.LocalDateTime;

public record VideoProgressBO(
        String courseId,
        String chapterId,
        String videoId,
        int videoVersion,
        long resumePositionMs,
        long maxPositionMs,
        long durationMs,
        long watchedSeconds,
        int completionRate,
        ProgressStatus status,
        long sessionEpoch,
        long sequence,
        LocalDateTime updatedAt) {
}
