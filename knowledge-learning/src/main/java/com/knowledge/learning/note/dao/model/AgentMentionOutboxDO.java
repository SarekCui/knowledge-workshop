package com.knowledge.learning.note.dao.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.knowledge.learning.note.enums.AgentMentionOutboxStatus;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("agent_mention_outbox")
public class AgentMentionOutboxDO {
    @TableId
    private String id;
    private String eventId;
    private String aggregateId;
    private String eventType;
    private String payload;
    private AgentMentionOutboxStatus status;
    private Integer retryCount;
    private LocalDateTime nextRetryAt;
    private String lastError;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
