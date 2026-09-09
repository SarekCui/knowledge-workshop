package com.knowledge.learning.progress.vo;

public record ProgressReportVO(
        String eventId,
        long sessionEpoch,
        long sequence,
        long resumePositionMs,
        boolean accepted) {
}
