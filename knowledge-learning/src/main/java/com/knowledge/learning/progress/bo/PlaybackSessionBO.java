package com.knowledge.learning.progress.bo;

public record PlaybackSessionBO(
        String sessionId,
        long sessionEpoch,
        String videoId,
        int videoVersion,
        long resumePositionMs) {
}
