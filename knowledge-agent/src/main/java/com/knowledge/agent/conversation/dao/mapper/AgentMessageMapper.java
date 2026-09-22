package com.knowledge.agent.conversation.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.agent.conversation.dao.model.AgentMessageDO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface AgentMessageMapper extends BaseMapper<AgentMessageDO> {

    @Select("SELECT * FROM agent_message WHERE conversation_id=#{conversationId} AND idempotency_key=#{idempotencyKey} LIMIT 1")
    AgentMessageDO findByConversationAndIdempotencyKey(@Param("conversationId") String conversationId,
            @Param("idempotencyKey") String idempotencyKey);
}
