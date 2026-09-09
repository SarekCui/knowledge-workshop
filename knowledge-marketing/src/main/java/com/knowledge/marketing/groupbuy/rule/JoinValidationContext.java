package com.knowledge.marketing.groupbuy.rule;

import com.knowledge.marketing.groupbuy.dao.model.GroupActivityDO;
import com.knowledge.marketing.groupbuy.dao.model.GroupOrderDO;
import com.knowledge.marketing.groupbuy.bo.JoinGroupBO;

public record JoinValidationContext(
        JoinGroupBO request,
        GroupActivityDO activity,
        GroupOrderDO group,
        long userJoinCount) {
}
