package com.knowledge.marketing.groupbuy.bo;

import com.knowledge.marketing.groupbuy.enums.GroupStatus;
import java.time.Instant;

public record GroupDetailBO(
        String id,
        String activityId,
        String ownerUserId,
        GroupStatus status,
        int targetCount,
        int confirmedCount,
        Instant expiresAt,
        int version) {
}
