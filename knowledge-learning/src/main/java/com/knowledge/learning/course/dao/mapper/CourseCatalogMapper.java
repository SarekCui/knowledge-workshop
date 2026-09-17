package com.knowledge.learning.course.dao.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowledge.learning.course.dao.model.CourseCatalogDO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface CourseCatalogMapper {

    @Select("""
            <script>
            SELECT c.id, c.title, c.summary, c.cover_url, c.price_cents,
                   c.category_id, cc.name AS category_name,
                   COUNT(ch.id) AS chapter_count,
                   COALESCE(SUM(ch.video_duration_ms), 0) AS total_duration_ms,
                   EXISTS (
                       SELECT 1
                         FROM course_entitlement entitlement
                        WHERE entitlement.user_id = #{userId}
                          AND entitlement.course_id = c.id
                          AND entitlement.status = 'ACTIVE'
                          AND entitlement.effective_at &lt;= UTC_TIMESTAMP(3)
                          AND (entitlement.expires_at IS NULL
                               OR entitlement.expires_at &gt; UTC_TIMESTAMP(3))
                   ) AS entitled,
                   c.updated_at
              FROM course c
              JOIN course_category cc ON cc.id = c.category_id AND cc.status = 'ACTIVE'
              LEFT JOIN chapter ch ON ch.course_id = c.id AND ch.status = 'PUBLISHED'
             WHERE c.status = 'PUBLISHED'
            <if test="categoryId != null and categoryId != ''">
               AND c.category_id = #{categoryId}
            </if>
            <if test="keyword != null and keyword != ''">
               AND (c.title LIKE CONCAT('%', #{keyword}, '%')
                    OR c.summary LIKE CONCAT('%', #{keyword}, '%'))
            </if>
             GROUP BY c.id, c.title, c.summary, c.cover_url, c.price_cents,
                      c.category_id, cc.name, c.updated_at
             ORDER BY c.updated_at DESC, c.id DESC
            </script>
            """)
    IPage<CourseCatalogDO> selectPublishedPage(Page<CourseCatalogDO> page,
            @Param("userId") String userId, @Param("categoryId") String categoryId,
            @Param("keyword") String keyword);

    @Select("""
            <script>
            SELECT c.id, c.title, c.summary, c.cover_url, c.price_cents,
                   c.category_id, cc.name AS category_name,
                   COUNT(ch.id) AS chapter_count,
                   COALESCE(SUM(ch.video_duration_ms), 0) AS total_duration_ms,
                   EXISTS (
                       SELECT 1
                         FROM course_entitlement entitlement
                        WHERE entitlement.user_id = #{userId}
                          AND entitlement.course_id = c.id
                          AND entitlement.status = 'ACTIVE'
                          AND entitlement.effective_at &lt;= UTC_TIMESTAMP(3)
                          AND (entitlement.expires_at IS NULL
                               OR entitlement.expires_at &gt; UTC_TIMESTAMP(3))
                   ) AS entitled,
                   c.updated_at
              FROM course c
              JOIN course_category cc ON cc.id = c.category_id AND cc.status = 'ACTIVE'
              LEFT JOIN chapter ch ON ch.course_id = c.id AND ch.status = 'PUBLISHED'
             WHERE c.id = #{courseId} AND c.status = 'PUBLISHED'
             GROUP BY c.id, c.title, c.summary, c.cover_url, c.price_cents,
                      c.category_id, cc.name, c.updated_at
            </script>
            """)
    CourseCatalogDO selectPublishedDetail(@Param("userId") String userId,
            @Param("courseId") String courseId);
}
