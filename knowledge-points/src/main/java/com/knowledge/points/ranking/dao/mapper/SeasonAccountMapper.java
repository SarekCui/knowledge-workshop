package com.knowledge.points.ranking.dao.mapper;

import com.knowledge.points.ranking.dao.model.SeasonAccountDO;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface SeasonAccountMapper {

    @Insert("""
            INSERT INTO season_account(season, user_id, points, version, updated_at)
            VALUES(#{season}, #{userId}, #{points}, 0, #{now})
            ON DUPLICATE KEY UPDATE points = points + #{points}, version = version + 1, updated_at = #{now}
            """)
    int addPoints(@Param("season") String season, @Param("userId") String userId,
                  @Param("points") int points, @Param("now") LocalDateTime now);

    @Select("""
            SELECT season, user_id, points, version, updated_at
              FROM season_account
             WHERE season = #{season} AND user_id = #{userId}
            """)
    SeasonAccountDO find(@Param("season") String season, @Param("userId") String userId);
}
