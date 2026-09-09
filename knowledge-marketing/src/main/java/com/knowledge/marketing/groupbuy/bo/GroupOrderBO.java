package com.knowledge.marketing.groupbuy.bo;

import com.knowledge.marketing.groupbuy.enums.TradeOrderStatus;

public record GroupOrderBO(String orderId, String groupId, TradeOrderStatus status, long amountCents) {
}
