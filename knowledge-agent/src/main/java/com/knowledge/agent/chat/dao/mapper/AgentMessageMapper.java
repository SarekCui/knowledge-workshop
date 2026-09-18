package com.knowledge.agent.chat.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.agent.chat.dao.model.AgentMessageDO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface AgentMessageMapper extends BaseMapper<AgentMessageDO> {

    @Select("SELECT * FROM agent_message WHERE conversation_id=#{conversationId} AND client_request_id=#{clientRequestId} LIMIT 1")
    AgentMessageDO findByRequest(@Param("conversationId") String conversationId,
            @Param("clientRequestId") String clientRequestId);
}
