package com.knowledge.agent.mention.dao.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.knowledge.agent.mention.enums.RunStatus;
import com.knowledge.agent.mention.enums.RunStage;
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
    private RunStatus status;
    private String eventId;
    private String conversationId;
    private String idempotencyKey;
    private String sourceCommentId;
    private String noteId;
    private String userId;
    private Integer attemptCount;
    private RunStage runStage;
    private String leaseOwner;
    private LocalDateTime leaseUntil;
    private Long executionVersion;
    private LocalDateTime nextRetryAt;
    private String answer;
    private String lastErrorCode;
    private String lastError;
    private LocalDateTime contextCompletedAt;
    private LocalDateTime generatedAt;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
