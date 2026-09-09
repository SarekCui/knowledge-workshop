package com.knowledge.learning.entitlement.bo;

import java.time.LocalDateTime;

public record CourseEntitlementBO(
        String id,
        String courseId,
        String sourceType,
        String sourceId,
        String status,
        LocalDateTime effectiveAt,
        LocalDateTime expiresAt) {
}
