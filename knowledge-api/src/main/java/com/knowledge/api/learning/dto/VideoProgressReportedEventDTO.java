package com.knowledge.api.learning.dto;

import java.time.Instant;
import java.util.List;

public record VideoProgressReportedEventDTO(
        String eventId,
        String userId,
        String courseId,
        String chapterId,
        String videoId,
        int videoVersion,
        String sessionId,
        long sessionEpoch,
        long sequence,
        String eventType,
        long positionMs,
        long durationMs,
        List<PlayedRangeEventDTO> playedRanges,
        Instant clientOccurredAt,
        Instant occurredAt,
        int version) {
}
