package com.knowledge.marketing.notification.converter;

import com.knowledge.marketing.notification.bo.NotificationTaskBO;
import com.knowledge.marketing.notification.dao.model.NotificationTaskDO;
import com.knowledge.marketing.notification.vo.NotificationTaskVO;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public final class NotificationTaskConverter {

    private NotificationTaskConverter() {
    }

    public static NotificationTaskBO toBO(NotificationTaskDO source) {
        return new NotificationTaskBO(source.getId(), source.getEventId(), source.getTaskType(), source.getStatus(),
                source.getRetryCount(), instant(source.getNextRetryAt()), source.getLastError(),
                instant(source.getCreatedAt()), instant(source.getUpdatedAt()));
    }

    public static NotificationTaskVO toVO(NotificationTaskBO source) {
        return new NotificationTaskVO(source.id(), source.eventId(), source.taskType(), source.status().name(),
                source.retryCount(), source.nextRetryAt(), source.lastError(), source.createdAt(), source.updatedAt());
    }

    private static java.time.Instant instant(LocalDateTime value) {
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }
}
