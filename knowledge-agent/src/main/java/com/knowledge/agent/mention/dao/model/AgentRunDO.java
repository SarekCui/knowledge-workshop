package com.knowledge.agent.mention.dao.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.knowledge.agent.mention.enums.AgentRunStatus;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("agent_run")
public class AgentRunDO {
    @TableId
    private String id;
    private String runType;
    private AgentRunStatus status;
    private String eventId;
    private String conversationId;
    private String clientRequestId;
    private String sourceCommentId;
    private String noteId;
    private String requesterId;
    private Integer attemptCount;
    private String lastError;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
