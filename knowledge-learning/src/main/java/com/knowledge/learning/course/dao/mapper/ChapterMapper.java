package com.knowledge.learning.course.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.learning.course.dao.model.ChapterDO;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface ChapterMapper extends BaseMapper<ChapterDO> {

    @Select("SELECT * FROM lr_chapter WHERE video_id = #{videoId} ORDER BY video_version DESC LIMIT 1")
    ChapterDO findLatestByVideoId(@Param("videoId") String videoId);

    @Update("""
            UPDATE lr_chapter
               SET title = #{title}, sort_order = #{sortOrder}, video_url = #{videoUrl},
                   video_duration_ms = #{durationMs}, status = #{status},
                   version = version + 1, updated_at = #{updatedAt}
             WHERE id = #{id} AND version = #{version}
            """)
    int updateDetails(@Param("id") String id, @Param("title") String title,
                      @Param("sortOrder") int sortOrder, @Param("videoUrl") String videoUrl,
                      @Param("durationMs") long durationMs, @Param("status") String status,
                      @Param("version") int version, @Param("updatedAt") LocalDateTime updatedAt);

    @Delete("DELETE FROM lr_chapter WHERE id = #{id} AND version = #{version}")
    int deleteWithVersion(@Param("id") String id, @Param("version") int version);
}
