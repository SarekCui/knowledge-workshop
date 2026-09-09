package com.knowledge.marketing.groupbuy.dao.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.knowledge.marketing.groupbuy.enums.TradeOrderStatus;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("trade_order")
public class TradeOrderDO {
    @TableId
    private String id;
    private String paymentTradeNo;
    private String userId;
    private String courseId;
    private String activityId;
    private String groupId;
    private Long amountCents;
    private TradeOrderStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
    private LocalDateTime updatedAt;
}
