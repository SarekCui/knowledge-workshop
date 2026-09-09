package com.knowledge.marketing.groupbuy.vo;

import io.swagger.v3.oas.annotations.media.Schema;

public record GroupOrderVO(
        @Schema(description = "交易订单 ID", example = "order-001") String orderId,
        @Schema(description = "团实例 ID", example = "group-demo") String groupId,
        @Schema(description = "订单状态", example = "PENDING_PAYMENT",
                allowableValues = {"PENDING_PAYMENT", "PAID", "FULFILLED", "CANCELLED", "CLOSED"}) String status,
        @Schema(description = "订单金额，单位为分", example = "9900") long amountCents) {

}
