package com.knowledge.marketing.groupbuy.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.marketing.groupbuy.dao.model.GroupActivityDO;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface GroupActivityMapper extends BaseMapper<GroupActivityDO> {

    @Update("""
            UPDATE group_activity
               SET course_id = #{courseId}, start_time = #{startTime}, end_time = #{endTime},
                   target_count = #{targetCount}, max_join_per_user = #{maxJoinPerUser},
                   price_cents = #{priceCents}, version = version + 1, updated_at = #{now}
             WHERE id = #{id} AND status = 'DRAFT' AND version = #{version}
            """)
    int updateDraft(@Param("id") String id, @Param("courseId") String courseId,
                    @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime,
                    @Param("targetCount") int targetCount, @Param("maxJoinPerUser") int maxJoinPerUser,
                    @Param("priceCents") long priceCents, @Param("version") int version,
                    @Param("now") LocalDateTime now);

    @Update("""
            UPDATE group_activity
               SET status = #{nextStatus}, version = version + 1, updated_at = #{now}
             WHERE id = #{id} AND status = #{expectedStatus} AND version = #{version}
            """)
    int changeStatus(@Param("id") String id, @Param("expectedStatus") String expectedStatus,
                     @Param("nextStatus") String nextStatus, @Param("version") int version,
                     @Param("now") LocalDateTime now);
}
