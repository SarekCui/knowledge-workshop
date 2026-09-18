package com.knowledge.agent.chat.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.agent.chat.dao.model.AgentConversationDO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface AgentConversationMapper extends BaseMapper<AgentConversationDO> {
    @Select("SELECT * FROM agent_conversation WHERE id = #{id} FOR UPDATE")
    AgentConversationDO selectForUpdate(@Param("id") String id);
}
