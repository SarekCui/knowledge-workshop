package com.knowledge.marketing.groupbuy.vo;

import java.time.Instant;

public record TradeOrderVO(
        String orderId,
        String courseId,
        String activityId,
        String groupId,
        long amountCents,
        String status,
        String paymentTradeNo,
        Instant createdAt,
        Instant paidAt) {
}
