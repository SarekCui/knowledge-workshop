package com.knowledge.learning.entitlement.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.learning.entitlement.dao.model.CourseEntitlementDO;
import org.apache.ibatis.annotations.Insert;

public interface CourseEntitlementMapper extends BaseMapper<CourseEntitlementDO> {

    @Insert("""
            INSERT IGNORE INTO lr_course_entitlement
              (id, user_id, course_id, source_type, source_id, status, effective_at,
               expires_at, created_at, updated_at)
            VALUES
              (#{id}, #{userId}, #{courseId}, #{sourceType}, #{sourceId}, #{status}, #{effectiveAt},
               #{expiresAt}, #{createdAt}, #{updatedAt})
            """)
    int insertIgnore(CourseEntitlementDO entitlement);
}
