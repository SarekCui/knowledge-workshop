package com.knowledge.learning.note.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.learning.note.dao.model.NoteDO;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface NoteMapper extends BaseMapper<NoteDO> {

    @Select("""
            SELECT * FROM note
             WHERE user_id = #{userId} AND client_request_id = #{clientRequestId}
             LIMIT 1
            """)
    NoteDO findByRequest(@Param("userId") String userId, @Param("clientRequestId") String clientRequestId);

    @Update("""
            UPDATE note
               SET title = #{title}, content = #{content}, video_position_ms = #{videoPositionMs},
                   version = version + 1, updated_at = #{updatedAt}
             WHERE id = #{id} AND user_id = #{userId} AND version = #{version} AND deleted = 0
            """)
    int updateContent(@Param("id") String id, @Param("userId") String userId,
                      @Param("title") String title, @Param("content") String content,
                      @Param("videoPositionMs") Long videoPositionMs, @Param("version") int version,
                      @Param("updatedAt") LocalDateTime updatedAt);

    @Update("""
            UPDATE note
               SET title = #{title}, version = version + 1, updated_at = #{updatedAt}
             WHERE id = #{id} AND user_id = #{userId} AND version = #{version} AND deleted = 0
            """)
    int rename(@Param("id") String id, @Param("userId") String userId, @Param("title") String title,
               @Param("version") int version, @Param("updatedAt") LocalDateTime updatedAt);

    @Update("""
            UPDATE note
               SET deleted = 1, version = version + 1, updated_at = #{updatedAt}
             WHERE id = #{id} AND user_id = #{userId} AND version = #{version} AND deleted = 0
            """)
    int softDelete(@Param("id") String id, @Param("userId") String userId,
                   @Param("version") int version, @Param("updatedAt") LocalDateTime updatedAt);

    @Select("SELECT COUNT(*) FROM note WHERE chapter_id = #{chapterId} AND deleted = 0")
    long countActiveByChapter(@Param("chapterId") String chapterId);
}
