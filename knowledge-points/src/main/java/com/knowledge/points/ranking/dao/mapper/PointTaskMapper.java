package com.knowledge.points.ranking.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.points.ranking.dao.model.PointTaskDO;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface PointTaskMapper extends BaseMapper<PointTaskDO> {

    @Select("""
            SELECT id, event_id, payload, status, retry_count, next_retry_at, last_error, created_at, updated_at
              FROM pt_point_task
             WHERE status IN ('PENDING', 'RETRY') AND next_retry_at <= #{now}
             ORDER BY created_at LIMIT #{limit}
            """)
    List<PointTaskDO> findDispatchable(@Param("now") LocalDateTime now, @Param("limit") int limit);

    @Update("""
            UPDATE pt_point_task SET status = 'SENDING', updated_at = #{now}
             WHERE id = #{id} AND status IN ('PENDING', 'RETRY')
            """)
    int claim(@Param("id") String id, @Param("now") LocalDateTime now);
}
