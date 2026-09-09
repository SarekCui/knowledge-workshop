package com.knowledge.learning.entitlement.dao.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.knowledge.learning.entitlement.enums.EntitlementStatus;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("lr_course_entitlement")
public class CourseEntitlementDO {
    @TableId
    private String id;
    private String userId;
    private String courseId;
    private String sourceType;
    private String sourceId;
    private EntitlementStatus status;
    private LocalDateTime effectiveAt;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
