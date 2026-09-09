package com.knowledge.marketing.groupbuy.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.marketing.groupbuy.dao.model.GroupParticipantDO;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface GroupParticipantMapper extends BaseMapper<GroupParticipantDO> {

    @Select("SELECT * FROM mk_group_participant WHERE request_id = #{requestId} LIMIT 1")
    GroupParticipantDO findByRequestId(@Param("requestId") String requestId);

    @Update("""
            UPDATE mk_group_participant SET status = 'CONFIRMED', updated_at = #{now}
             WHERE request_id = #{requestId} AND status = 'RESERVED'
            """)
    int confirmByRequestId(@Param("requestId") String requestId, @Param("now") LocalDateTime now);

    @Update("""
            UPDATE mk_group_participant SET status = 'RELEASED', updated_at = #{now}
             WHERE request_id = #{requestId} AND status = 'RESERVED'
            """)
    int releaseByRequestId(@Param("requestId") String requestId, @Param("now") LocalDateTime now);

    @Select("""
            SELECT user_id FROM mk_group_participant
             WHERE group_id = #{groupId} AND status = 'CONFIRMED'
             ORDER BY created_at, id
            """)
    List<String> findConfirmedUserIds(@Param("groupId") String groupId);
}
