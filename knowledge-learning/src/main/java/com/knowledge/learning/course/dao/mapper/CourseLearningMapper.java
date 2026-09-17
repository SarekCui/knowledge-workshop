package com.knowledge.learning.course.dao.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowledge.learning.course.dao.model.CourseLearningDO;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface CourseLearningMapper {
    String PROJECTION = """
            SELECT c.id AS course_id, c.title, c.cover_url,
              (SELECT COUNT(*) FROM chapter ch WHERE ch.course_id=c.id AND ch.status='PUBLISHED'
                AND ch.video_version=(SELECT MAX(v.video_version) FROM chapter v WHERE v.video_id=ch.video_id)) AS total_videos,
              (SELECT COUNT(*) FROM chapter ch JOIN video_progress vp
                ON vp.chapter_id=ch.id AND vp.video_id=ch.video_id AND vp.video_version=ch.video_version
                AND vp.user_id=#{userId} AND vp.course_id=c.id AND vp.status='COMPLETED'
                WHERE ch.course_id=c.id AND ch.status='PUBLISHED'
                AND ch.video_version=(SELECT MAX(v.video_version) FROM chapter v WHERE v.video_id=ch.video_id)) AS completed_videos,
              COALESCE(p.chapter_id, first_ch.id) AS chapter_id,
              COALESCE(p.video_id, first_ch.video_id) AS video_id,
              COALESCE(p.video_version, first_ch.video_version) AS video_version,
              COALESCE(p.resume_position_ms, 0) AS resume_position_ms,
              p.updated_at AS last_learned_at
            FROM course c
            LEFT JOIN video_progress p ON p.id=(
              SELECT vp.id FROM video_progress vp JOIN chapter ch ON ch.id=vp.chapter_id
                AND ch.video_id=vp.video_id AND ch.video_version=vp.video_version
              WHERE vp.user_id=#{userId} AND vp.course_id=c.id AND ch.course_id=c.id
                AND ch.status='PUBLISHED' AND vp.last_event_id IS NOT NULL
                AND ch.video_version=(SELECT MAX(v.video_version) FROM chapter v WHERE v.video_id=ch.video_id)
              ORDER BY vp.updated_at DESC, vp.id ASC LIMIT 1)
            LEFT JOIN chapter first_ch ON first_ch.id=(
              SELECT ch.id FROM chapter ch WHERE ch.course_id=c.id AND ch.status='PUBLISHED'
                AND ch.video_version=(SELECT MAX(v.video_version) FROM chapter v WHERE v.video_id=ch.video_id)
              ORDER BY ch.sort_order, ch.id LIMIT 1)
            WHERE c.status='PUBLISHED'
              AND EXISTS (SELECT 1 FROM course_entitlement e WHERE e.course_id=c.id AND e.user_id=#{userId}
                AND e.status='ACTIVE' AND e.effective_at <= #{now}
                AND (e.expires_at IS NULL OR e.expires_at > #{now}))
            """;

    @Select(PROJECTION + " ORDER BY COALESCE(p.updated_at, c.created_at) DESC, c.id ASC")
    Page<CourseLearningDO> pageMine(Page<CourseLearningDO> page, @Param("userId") String userId,
            @Param("now") LocalDateTime now);

    @Select(PROJECTION + " AND c.id=#{courseId}")
    CourseLearningDO findMine(@Param("userId") String userId, @Param("courseId") String courseId,
            @Param("now") LocalDateTime now);
}
