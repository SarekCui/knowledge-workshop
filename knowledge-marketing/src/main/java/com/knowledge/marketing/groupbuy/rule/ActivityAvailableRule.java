package com.knowledge.marketing.groupbuy.rule;

import com.knowledge.common.exception.BusinessException;
import com.knowledge.marketing.groupbuy.enums.ActivityStatus;
import java.time.Clock;
import java.time.LocalDateTime;
import jakarta.annotation.Resource;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Order(10)
@Component
public class ActivityAvailableRule implements JoinRule {

    @Resource private Clock clock;


    @Override
    public void check(JoinValidationContext context) {
        if (context.activity().getStatus() != ActivityStatus.ACTIVE) {
            throw BusinessException.conflict("拼团活动未开启");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (now.isBefore(context.activity().getStartTime())) {
            throw BusinessException.conflict("拼团活动尚未开始");
        }
        if (!now.isBefore(context.activity().getEndTime())) {
            throw BusinessException.conflict("拼团活动已结束");
        }
    }
}
