package com.knowledge.marketing.groupbuy.converter;

import com.knowledge.marketing.groupbuy.bo.GroupOrderBO;
import com.knowledge.marketing.groupbuy.vo.GroupOrderVO;

public final class GroupOrderConverter {

    private GroupOrderConverter() {
    }

    public static GroupOrderVO toVO(GroupOrderBO order) {
        return new GroupOrderVO(order.orderId(), order.groupId(), order.status().name(), order.amountCents());
    }
}
