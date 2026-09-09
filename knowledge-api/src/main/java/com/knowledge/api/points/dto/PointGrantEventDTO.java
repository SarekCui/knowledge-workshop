package com.knowledge.api.points.dto;

import java.time.Instant;

public record PointGrantEventDTO(
        String eventId,
        String userId,
        int points,
        String sourceType,
        String sourceId,
        String requestId,
        Instant occurredAt,
        int version) {
}
