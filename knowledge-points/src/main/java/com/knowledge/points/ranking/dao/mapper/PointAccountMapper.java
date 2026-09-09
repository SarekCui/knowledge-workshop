package com.knowledge.points.ranking.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.points.ranking.dao.model.PointAccountDO;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

public interface PointAccountMapper extends BaseMapper<PointAccountDO> {

    @Insert("""
            INSERT INTO pt_point_account(user_id, total_points, version, updated_at)
            VALUES(#{userId}, #{points}, 0, #{now})
            ON DUPLICATE KEY UPDATE total_points = total_points + #{points},
                                    version = version + 1, updated_at = #{now}
            """)
    int addPoints(@Param("userId") String userId, @Param("points") int points, @Param("now") LocalDateTime now);
}
