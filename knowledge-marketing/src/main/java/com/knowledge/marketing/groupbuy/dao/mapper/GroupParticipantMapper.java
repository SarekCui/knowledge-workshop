package com.knowledge.marketing.groupbuy.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.marketing.groupbuy.dao.model.GroupParticipantDO;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface GroupParticipantMapper extends BaseMapper<GroupParticipantDO> {

    @Select("SELECT * FROM group_participant WHERE group_id = #{groupId} AND user_id = #{userId} LIMIT 1")
    GroupParticipantDO findByGroupAndUser(@Param("groupId") String groupId, @Param("userId") String userId);

    @Update("""
            UPDATE group_participant SET status = 'CONFIRMED', updated_at = #{now}
             WHERE group_id = #{groupId} AND user_id = #{userId} AND status = 'RESERVED'
            """)
    int confirmByGroupAndUser(@Param("groupId") String groupId, @Param("userId") String userId,
                              @Param("now") LocalDateTime now);

    @Update("""
            UPDATE group_participant SET status = 'RELEASED', updated_at = #{now}
             WHERE group_id = #{groupId} AND user_id = #{userId} AND status = 'RESERVED'
            """)
    int releaseByGroupAndUser(@Param("groupId") String groupId, @Param("userId") String userId,
                              @Param("now") LocalDateTime now);

    @Select("""
            SELECT user_id FROM group_participant
             WHERE group_id = #{groupId} AND status = 'CONFIRMED'
             ORDER BY created_at, id
            """)
    List<String> findConfirmedUserIds(@Param("groupId") String groupId);
}
