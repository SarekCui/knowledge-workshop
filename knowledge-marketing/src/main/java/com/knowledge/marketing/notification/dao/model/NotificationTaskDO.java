package com.knowledge.marketing.notification.dao.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.knowledge.marketing.notification.enums.NotificationStatus;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("mk_notification_task")
public class NotificationTaskDO {
    @TableId
    private String id;
    private String eventId;
    private String taskType;
    private String payload;
    private NotificationStatus status;
    private Integer retryCount;
    private LocalDateTime nextRetryAt;
    private String lastError;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
