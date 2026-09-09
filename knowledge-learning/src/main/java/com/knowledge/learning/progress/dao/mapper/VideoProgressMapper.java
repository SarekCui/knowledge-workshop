package com.knowledge.learning.progress.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.learning.progress.dao.model.VideoProgressDO;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface VideoProgressMapper extends BaseMapper<VideoProgressDO> {

    @Insert("""
            INSERT IGNORE INTO lr_video_progress
              (id, user_id, course_id, chapter_id, video_id, video_version,
               resume_position_ms, max_position_ms, duration_ms, watched_seconds,
               completion_rate, status, last_session_epoch, last_sequence,
               version, created_at, updated_at)
            VALUES
              (#{id}, #{userId}, #{courseId}, #{chapterId}, #{videoId}, #{videoVersion},
               0, 0, #{durationMs}, 0, 0, 'NOT_STARTED', 0, 0, 0, #{now}, #{now})
            """)
    int ensureRow(@Param("id") String id, @Param("userId") String userId,
                  @Param("courseId") String courseId, @Param("chapterId") String chapterId,
                  @Param("videoId") String videoId, @Param("videoVersion") int videoVersion,
                  @Param("durationMs") long durationMs, @Param("now") LocalDateTime now);

    @Select("""
            SELECT * FROM lr_video_progress
             WHERE user_id = #{userId} AND video_id = #{videoId} AND video_version = #{videoVersion}
             FOR UPDATE
            """)
    VideoProgressDO findForUpdate(@Param("userId") String userId, @Param("videoId") String videoId,
                                  @Param("videoVersion") int videoVersion);

    @Select("""
            SELECT * FROM lr_video_progress
             WHERE user_id = #{userId} AND video_id = #{videoId} AND video_version = #{videoVersion}
             LIMIT 1
            """)
    VideoProgressDO findOne(@Param("userId") String userId, @Param("videoId") String videoId,
                            @Param("videoVersion") int videoVersion);

    @Select("SELECT COUNT(*) FROM lr_video_progress WHERE chapter_id = #{chapterId}")
    long countByChapter(@Param("chapterId") String chapterId);
}
