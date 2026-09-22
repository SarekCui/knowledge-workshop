package com.knowledge.agent.mention.dao.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.knowledge.agent.mention.enums.ExecutionOutboxStatus;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("agent_execution_outbox")
public class AgentExecutionOutboxDO {
    @TableId
    private String id;
    private String runId;
    private Long executionVersion;
    private String routingKey;
    private String payload;
    private ExecutionOutboxStatus status;
    private Integer retryCount;
    private LocalDateTime nextRetryAt;
    private String lastError;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
