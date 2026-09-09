package com.knowledge.marketing.groupbuy.rule;

import com.knowledge.common.exception.BusinessException;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Order(20)
@Component
public class ParticipationLimitRule implements JoinRule {

    @Override
    public void check(JoinValidationContext context) {
        if (context.userJoinCount() >= context.activity().getMaxJoinPerUser()) {
            throw BusinessException.conflict("已达到该活动的参与次数上限");
        }
    }
}
