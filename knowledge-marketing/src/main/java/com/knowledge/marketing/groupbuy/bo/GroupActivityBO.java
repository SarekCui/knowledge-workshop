package com.knowledge.marketing.groupbuy.bo;

import com.knowledge.marketing.groupbuy.enums.ActivityStatus;
import java.time.Instant;

public record GroupActivityBO(
        String id,
        String courseId,
        ActivityStatus status,
        Instant startTime,
        Instant endTime,
        int targetCount,
        int maxJoinPerUser,
        long priceCents,
        int version) {
}
