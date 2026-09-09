package com.knowledge.marketing.notification.vo;

import java.time.Instant;

public record NotificationTaskVO(
        String id,
        String eventId,
        String taskType,
        String status,
        int retryCount,
        Instant nextRetryAt,
        String lastError,
        Instant createdAt,
        Instant updatedAt) {
}
