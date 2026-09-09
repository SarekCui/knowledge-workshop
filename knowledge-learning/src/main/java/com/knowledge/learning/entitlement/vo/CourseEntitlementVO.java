package com.knowledge.learning.entitlement.vo;

import java.time.LocalDateTime;

public record CourseEntitlementVO(
        String id,
        String courseId,
        String sourceType,
        String sourceId,
        String status,
        LocalDateTime effectiveAt,
        LocalDateTime expiresAt) {
}
