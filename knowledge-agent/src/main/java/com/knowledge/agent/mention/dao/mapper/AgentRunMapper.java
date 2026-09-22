package com.knowledge.agent.mention.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.agent.mention.dao.model.AgentRunDO;

import java.time.LocalDateTime;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface AgentRunMapper extends BaseMapper<AgentRunDO> {

    @Insert("""
            INSERT IGNORE INTO agent_run(id,run_type,status,event_id,conversation_id,idempotency_key,source_comment_id,note_id,user_id,
                                         attempt_count,last_error,created_at,updated_at)
            VALUES(#{id},#{runType},#{status},#{eventId},#{conversationId},#{idempotencyKey},#{sourceCommentId},#{noteId},#{userId},
                    #{attemptCount},#{lastError},#{createdAt},#{updatedAt})
            """)
    int insertIgnore(AgentRunDO run);

    @Update("""
            UPDATE agent_run
               SET status = 'RUNNING', lease_owner = #{owner}, lease_until = #{leaseUntil},
                   execution_version = execution_version + 1, attempt_count = attempt_count + 1,
                   last_error_code = NULL, last_error = NULL, updated_at = #{now}
             WHERE id = #{id} AND status IN ('PENDING', 'RETRY_WAIT')
               AND (next_retry_at IS NULL OR next_retry_at <= #{now})
            """)
    int claim(@Param("id") String id, @Param("owner") String owner,
              @Param("leaseUntil") LocalDateTime leaseUntil, @Param("now") LocalDateTime now);

    @Update("""
            UPDATE agent_run SET lease_until = #{leaseUntil}, updated_at = #{now}
             WHERE id = #{id} AND status = 'RUNNING' AND execution_version = #{executionVersion}
               AND lease_owner = #{owner} AND lease_until > #{now}
            """)
    int renewLease(@Param("id") String id, @Param("executionVersion") long executionVersion,
                   @Param("owner") String owner, @Param("leaseUntil") LocalDateTime leaseUntil,
                   @Param("now") LocalDateTime now);

    @Update("""
            UPDATE agent_run SET run_stage = 'GENERATE', context_completed_at = #{now}, updated_at = #{now}
             WHERE id = #{id} AND status = 'RUNNING' AND execution_version = #{executionVersion}
               AND run_stage = 'CONTEXT'
            """)
    int startGeneration(@Param("id") String id, @Param("executionVersion") long executionVersion,
                        @Param("now") LocalDateTime now);

    @Update("""
            UPDATE agent_run
               SET run_stage = 'PUBLISH', answer = #{answer}, generated_at = #{now}, updated_at = #{now}
             WHERE id = #{id} AND status = 'RUNNING' AND execution_version = #{executionVersion}
               AND run_stage IN ('CONTEXT', 'GENERATE')
            """)
    int persistGeneratedAnswer(@Param("id") String id, @Param("executionVersion") long executionVersion,
                               @Param("answer") String answer, @Param("now") LocalDateTime now);

    @Update("""
            UPDATE agent_run
               SET status = 'SUCCEEDED', published_at = #{now}, lease_owner = NULL, lease_until = NULL,
                   next_retry_at = NULL, updated_at = #{now}
             WHERE id = #{id} AND status = 'RUNNING' AND run_stage = 'PUBLISH'
               AND execution_version = #{executionVersion}
            """)
    int markPublished(@Param("id") String id, @Param("executionVersion") long executionVersion,
                      @Param("now") LocalDateTime now);

    @Update("""
            UPDATE agent_run
               SET status = 'RETRY_WAIT', lease_owner = NULL, lease_until = NULL, next_retry_at = #{nextRetryAt},
                   last_error_code = #{errorCode}, last_error = #{error}, updated_at = #{now}
             WHERE id = #{id} AND status = 'RUNNING' AND execution_version = #{executionVersion}
            """)
    int scheduleRetry(@Param("id") String id, @Param("executionVersion") long executionVersion,
                      @Param("nextRetryAt") LocalDateTime nextRetryAt, @Param("errorCode") String errorCode,
                      @Param("error") String error, @Param("now") LocalDateTime now);

    @Update("""
            UPDATE agent_run
               SET status = 'DEAD', lease_owner = NULL, lease_until = NULL,
                   last_error_code = #{errorCode}, last_error = #{error}, updated_at = #{now}
             WHERE id = #{id} AND status = 'RUNNING' AND execution_version = #{executionVersion}
            """)
    int markDead(@Param("id") String id, @Param("executionVersion") long executionVersion,
                 @Param("errorCode") String errorCode, @Param("error") String error, @Param("now") LocalDateTime now);

    @Select("""
            SELECT * FROM agent_run
             WHERE status = 'RETRY_WAIT' AND next_retry_at <= #{now}
             ORDER BY next_retry_at, id LIMIT #{limit}
            """)
    java.util.List<AgentRunDO> findRetryDue(@Param("now") LocalDateTime now, @Param("limit") int limit);

    @Select("""
            SELECT * FROM agent_run
             WHERE status = 'RUNNING' AND lease_until < #{now}
             ORDER BY lease_until, id LIMIT #{limit}
            """)
    java.util.List<AgentRunDO> findExpiredLease(@Param("now") LocalDateTime now, @Param("limit") int limit);

    @Update("""
            UPDATE agent_run SET status = 'PENDING', next_retry_at = NULL, updated_at = #{now}
             WHERE id = #{id} AND status = 'RETRY_WAIT' AND execution_version = #{executionVersion}
               AND next_retry_at <= #{now}
            """)
    int activateRetry(@Param("id") String id, @Param("executionVersion") long executionVersion,
                      @Param("now") LocalDateTime now);

    @Update("""
            UPDATE agent_run SET status = 'PENDING', lease_owner = NULL, lease_until = NULL, updated_at = #{now}
             WHERE id = #{id} AND status = 'RUNNING' AND execution_version = #{executionVersion}
               AND lease_until < #{now}
            """)
    int recoverExpiredLease(@Param("id") String id, @Param("executionVersion") long executionVersion,
                            @Param("now") LocalDateTime now);
}
