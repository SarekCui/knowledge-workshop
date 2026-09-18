package com.knowledge.agent.mention.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.agent.mention.dao.model.AgentRunDO;
import org.apache.ibatis.annotations.Insert;

public interface AgentRunMapper extends BaseMapper<AgentRunDO> {

    @Insert("""
            INSERT IGNORE INTO agent_run(id,run_type,status,event_id,conversation_id,client_request_id,source_comment_id,note_id,requester_id,
                                         attempt_count,last_error,created_at,updated_at)
            VALUES(#{id},#{runType},#{status},#{eventId},#{conversationId},#{clientRequestId},#{sourceCommentId},#{noteId},#{requesterId},
                    #{attemptCount},#{lastError},#{createdAt},#{updatedAt})
            """)
    int insertIgnore(AgentRunDO run);
}
