package com.knowledge.points.season.rule;

import com.knowledge.common.exception.BusinessException;
import java.time.Instant;

public final class SeasonSettlementRule {
    private SeasonSettlementRule() {
    }

    public static void validate(Instant endsAt, Instant now, long outstandingTasks) {
        if (now.isBefore(endsAt)) {
            throw BusinessException.conflict("赛季尚未结束，不能结算");
        }
        if (outstandingTasks > 0) {
            throw BusinessException.conflict("赛季仍有未入账积分任务，请补偿完成后重试");
        }
    }
}
