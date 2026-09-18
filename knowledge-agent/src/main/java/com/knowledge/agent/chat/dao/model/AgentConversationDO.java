package com.knowledge.agent.chat.dao.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("agent_conversation")
public class AgentConversationDO {
    @TableId
    private String id;
    private String userId;
    private String title;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
