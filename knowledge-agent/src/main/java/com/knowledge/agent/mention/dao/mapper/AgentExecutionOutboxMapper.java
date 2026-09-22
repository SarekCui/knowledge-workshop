package com.knowledge.agent.mention.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.agent.mention.dao.model.AgentExecutionOutboxDO;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface AgentExecutionOutboxMapper extends BaseMapper<AgentExecutionOutboxDO> {

    @Select("""
            SELECT * FROM agent_execution_outbox
             WHERE status IN ('PENDING', 'RETRY') AND next_retry_at <= #{now}
             ORDER BY created_at, id LIMIT #{limit}
            """)
    List<AgentExecutionOutboxDO> findDispatchable(@Param("now") LocalDateTime now, @Param("limit") int limit);

    @Update("""
            UPDATE agent_execution_outbox SET status = 'SENDING', updated_at = #{now}
             WHERE id = #{id} AND status IN ('PENDING', 'RETRY') AND next_retry_at <= #{now}
            """)
    int claim(@Param("id") String id, @Param("now") LocalDateTime now);

    @Update("""
            UPDATE agent_execution_outbox SET status = 'RETRY', next_retry_at = #{now}, updated_at = #{now}
             WHERE status = 'SENDING' AND updated_at < #{staleBefore}
            """)
    int recoverStaleSending(@Param("staleBefore") LocalDateTime staleBefore, @Param("now") LocalDateTime now);
}
