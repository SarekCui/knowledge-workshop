package com.knowledge.marketing.groupbuy.rule;

import com.knowledge.marketing.groupbuy.dao.model.GroupActivityDO;
import com.knowledge.marketing.groupbuy.dao.model.GroupOrderDO;
public record JoinValidationContext(
        GroupActivityDO activity,
        GroupOrderDO group,
        long userJoinCount) {
}
