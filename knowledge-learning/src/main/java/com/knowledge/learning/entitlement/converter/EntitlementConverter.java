package com.knowledge.learning.entitlement.converter;

import com.knowledge.learning.entitlement.bo.CourseEntitlementBO;
import com.knowledge.learning.entitlement.dao.model.CourseEntitlementDO;
import com.knowledge.learning.entitlement.vo.CourseEntitlementVO;

public final class EntitlementConverter {

    private EntitlementConverter() {
    }

    public static CourseEntitlementBO toBO(CourseEntitlementDO source) {
        return new CourseEntitlementBO(source.getId(), source.getCourseId(), source.getSourceType(),
                source.getSourceId(), source.getStatus().name(), source.getEffectiveAt(), source.getExpiresAt());
    }

    public static CourseEntitlementVO toVO(CourseEntitlementBO source) {
        return new CourseEntitlementVO(source.id(), source.courseId(), source.sourceType(), source.sourceId(),
                source.status(), source.effectiveAt(), source.expiresAt());
    }
}
