package com.knowledge.marketing.notification.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.marketing.notification.dao.model.NotificationTaskDO;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface NotificationTaskMapper extends BaseMapper<NotificationTaskDO> {

    @Select("""
            SELECT * FROM mk_notification_task
             WHERE status IN ('PENDING', 'RETRY') AND next_retry_at <= #{now}
             ORDER BY created_at LIMIT #{limit}
            """)
    List<NotificationTaskDO> findDispatchable(@Param("now") LocalDateTime now, @Param("limit") int limit);

    @Update("""
            UPDATE mk_notification_task SET status = 'SENDING', updated_at = #{now}
             WHERE id = #{id} AND status IN ('PENDING', 'RETRY')
            """)
    int claim(@Param("id") String id, @Param("now") LocalDateTime now);
}
