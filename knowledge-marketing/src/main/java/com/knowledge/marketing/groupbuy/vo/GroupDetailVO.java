package com.knowledge.marketing.groupbuy.vo;

import java.time.Instant;

public record GroupDetailVO(
        String id,
        String activityId,
        String ownerUserId,
        String status,
        int targetCount,
        int confirmedCount,
        Instant expiresAt,
        int version) {
}
