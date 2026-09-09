package com.knowledge.learning.progress.bo;

public record ProgressReportBO(
        String eventId,
        long sessionEpoch,
        long sequence,
        long resumePositionMs,
        boolean accepted) {
}
