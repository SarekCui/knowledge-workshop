package com.knowledge.learning.progress.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.learning.progress.dao.model.VideoProgressDO;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface VideoProgressMapper extends BaseMapper<VideoProgressDO> {
    @Select("""
            SELECT p.* FROM video_progress p
            JOIN chapter ch ON ch.id=p.chapter_id AND ch.video_id=p.video_id AND ch.video_version=p.video_version
            JOIN course c ON c.id=p.course_id AND ch.course_id=c.id
            WHERE p.user_id=#{userId} AND p.last_event_id IS NOT NULL
              AND ch.status='PUBLISHED' AND c.status='PUBLISHED'
              AND ch.video_version=(SELECT MAX(v.video_version) FROM chapter v WHERE v.video_id=ch.video_id)
              AND EXISTS (SELECT 1 FROM course_entitlement e WHERE e.user_id=p.user_id AND e.course_id=c.id
                AND e.status='ACTIVE' AND e.effective_at <= #{now} AND (e.expires_at IS NULL OR e.expires_at > #{now}))
            ORDER BY p.updated_at DESC, p.id ASC LIMIT #{limit}
            """)
    List<VideoProgressDO> findRecentAvailable(@Param("userId") String userId,
            @Param("now") LocalDateTime now, @Param("limit") int limit);

    @Insert("""
            INSERT IGNORE INTO video_progress
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
            SELECT * FROM video_progress
             WHERE user_id = #{userId} AND video_id = #{videoId} AND video_version = #{videoVersion}
             FOR UPDATE
            """)
    VideoProgressDO findForUpdate(@Param("userId") String userId, @Param("videoId") String videoId,
                                  @Param("videoVersion") int videoVersion);

    @Select("""
            SELECT * FROM video_progress
             WHERE user_id = #{userId} AND video_id = #{videoId} AND video_version = #{videoVersion}
             LIMIT 1
            """)
    VideoProgressDO findOne(@Param("userId") String userId, @Param("videoId") String videoId,
                            @Param("videoVersion") int videoVersion);

    @Select("SELECT COUNT(*) FROM video_progress WHERE chapter_id = #{chapterId}")
    long countByChapter(@Param("chapterId") String chapterId);
}
