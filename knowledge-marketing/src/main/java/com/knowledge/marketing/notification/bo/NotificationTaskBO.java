package com.knowledge.marketing.notification.bo;

import com.knowledge.marketing.notification.enums.NotificationStatus;
import java.time.Instant;

public record NotificationTaskBO(
        String id,
        String eventId,
        String taskType,
        NotificationStatus status,
        int retryCount,
        Instant nextRetryAt,
        String lastError,
        Instant createdAt,
        Instant updatedAt) {
}
