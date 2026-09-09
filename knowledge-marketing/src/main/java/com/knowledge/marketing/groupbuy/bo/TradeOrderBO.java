package com.knowledge.marketing.groupbuy.bo;

import com.knowledge.marketing.groupbuy.enums.TradeOrderStatus;
import java.time.Instant;

public record TradeOrderBO(
        String orderId,
        String userId,
        String courseId,
        String activityId,
        String groupId,
        long amountCents,
        TradeOrderStatus status,
        String paymentTradeNo,
        Instant createdAt,
        Instant paidAt) {
}
