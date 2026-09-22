package com.knowledge.agent.conversation.dao.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("agent_message")
public class AgentMessageDO {
    @TableId
    private String id;
    private String conversationId;
    private String runId;
    private String role;
    private String idempotencyKey;
    private String content;
    private LocalDateTime createdAt;
}
