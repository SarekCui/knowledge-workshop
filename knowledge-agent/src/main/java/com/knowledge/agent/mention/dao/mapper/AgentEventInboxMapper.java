package com.knowledge.agent.mention.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.agent.mention.dao.model.AgentEventInboxDO;
import org.apache.ibatis.annotations.Insert;

public interface AgentEventInboxMapper extends BaseMapper<AgentEventInboxDO> {

    @Insert("""
            INSERT IGNORE INTO agent_event_inbox(id,event_id,consumer,created_at)
            VALUES(#{id},#{eventId},#{consumer},#{createdAt})
            """)
    int insertIgnore(AgentEventInboxDO inbox);
}
