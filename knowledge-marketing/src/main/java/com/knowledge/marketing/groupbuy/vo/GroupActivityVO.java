package com.knowledge.marketing.groupbuy.vo;

import java.time.Instant;

public record GroupActivityVO(
        String id,
        String courseId,
        String status,
        Instant startTime,
        Instant endTime,
        int targetCount,
        int maxJoinPerUser,
        long priceCents,
        int version) {
}
