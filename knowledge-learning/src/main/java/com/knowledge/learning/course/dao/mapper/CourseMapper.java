package com.knowledge.learning.course.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.learning.course.dao.model.CourseDO;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface CourseMapper extends BaseMapper<CourseDO> {

    @Update("""
            UPDATE course
               SET category_id = #{categoryId}, title = #{title}, summary = #{summary}, cover_url = #{coverUrl},
                   price_cents = #{priceCents}, version = version + 1, updated_at = #{updatedAt}
             WHERE id = #{id} AND version = #{version}
            """)
    int updateDetails(@Param("id") String id, @Param("categoryId") String categoryId,
                      @Param("title") String title,
                      @Param("summary") String summary, @Param("coverUrl") String coverUrl,
                      @Param("priceCents") long priceCents, @Param("version") int version,
                      @Param("updatedAt") LocalDateTime updatedAt);

    @Update("""
            UPDATE course
               SET status = #{status}, version = version + 1, updated_at = #{updatedAt}
             WHERE id = #{id} AND version = #{version}
            """)
    int updateStatus(@Param("id") String id, @Param("status") String status,
                     @Param("version") int version, @Param("updatedAt") LocalDateTime updatedAt);
}
