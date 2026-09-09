package com.knowledge.marketing.groupbuy.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.marketing.groupbuy.dao.model.GroupOrderDO;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface GroupOrderMapper extends BaseMapper<GroupOrderDO> {

    @Update("""
            UPDATE group_order
               SET status = CASE WHEN confirmed_count + 1 >= target_count THEN 'FORMED' ELSE status END,
                   confirmed_count = confirmed_count + 1,
                   version = version + 1,
                   updated_at = #{now}
             WHERE id = #{groupId}
               AND status = 'FORMING'
               AND confirmed_count < target_count
            """)
    int confirmOne(@Param("groupId") String groupId, @Param("now") LocalDateTime now);
}
