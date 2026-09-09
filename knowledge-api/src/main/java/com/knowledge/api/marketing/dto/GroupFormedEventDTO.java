package com.knowledge.api.marketing.dto;

import java.time.Instant;
import java.util.List;

public record GroupFormedEventDTO(
        String eventId,
        String groupId,
        String activityId,
        String courseId,
        List<String> userIds,
        String requestId,
        Instant occurredAt,
        int version) {
}
