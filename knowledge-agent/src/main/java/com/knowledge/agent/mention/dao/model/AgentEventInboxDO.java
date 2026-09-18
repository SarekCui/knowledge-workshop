package com.knowledge.agent.mention.dao.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("agent_event_inbox")
public class AgentEventInboxDO {
    @TableId
    private String id;
    private String eventId;
    private String consumer;
    private LocalDateTime createdAt;
}
