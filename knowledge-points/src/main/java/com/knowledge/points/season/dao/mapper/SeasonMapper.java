package com.knowledge.points.season.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.points.season.dao.model.SeasonDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface SeasonMapper extends BaseMapper<SeasonDO> {
    @Select("SELECT * FROM season WHERE season = #{season} FOR UPDATE")
    SeasonDO lock(@Param("season") String season);

    @Update("UPDATE season SET status = 'FAILED', last_error = #{error} WHERE season = #{season} AND status <> 'SETTLED'")
    int markFailed(@Param("season") String season, @Param("error") String error);
    @Insert("""
            INSERT INTO season(season, name, starts_at, ends_at, created_at)
            VALUES(#{item.season}, #{item.name}, #{item.startsAt}, #{item.endsAt}, #{item.createdAt})
            ON DUPLICATE KEY UPDATE season = VALUES(season)
            """)
    int insertIfAbsent(@Param("item") SeasonDO item);
}
