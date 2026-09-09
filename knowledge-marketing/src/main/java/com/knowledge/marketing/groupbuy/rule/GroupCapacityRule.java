package com.knowledge.marketing.groupbuy.rule;

import com.knowledge.common.exception.BusinessException;
import com.knowledge.marketing.groupbuy.enums.GroupStatus;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Order(30)
@Component
public class GroupCapacityRule implements JoinRule {

    private final Clock clock;

    public GroupCapacityRule(Clock clock) {
        this.clock = clock;
    }

    @Override
    public void check(JoinValidationContext context) {
        if (context.group().getStatus() != GroupStatus.FORMING) {
            throw BusinessException.conflict("拼团已结束");
        }
        if (!LocalDateTime.now(clock).isBefore(context.group().getExpiresAt())) {
            throw BusinessException.conflict("拼团已过期");
        }
        if (context.group().getConfirmedCount() >= context.group().getTargetCount()) {
            throw BusinessException.conflict("拼团名额已满");
        }
    }
}
