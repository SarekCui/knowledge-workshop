package com.knowledge.learning.progress.vo;

public record PlaybackSessionVO(
        String sessionId,
        long sessionEpoch,
        String videoId,
        int videoVersion,
        long resumePositionMs) {
}
