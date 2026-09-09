package com.knowledge.marketing.groupbuy.converter;

import com.knowledge.marketing.groupbuy.bo.GroupActivityBO;
import com.knowledge.marketing.groupbuy.bo.GroupDetailBO;
import com.knowledge.marketing.groupbuy.bo.TradeOrderBO;
import com.knowledge.marketing.groupbuy.dao.model.GroupActivityDO;
import com.knowledge.marketing.groupbuy.dao.model.GroupOrderDO;
import com.knowledge.marketing.groupbuy.dao.model.TradeOrderDO;
import com.knowledge.marketing.groupbuy.vo.GroupActivityVO;
import com.knowledge.marketing.groupbuy.vo.GroupDetailVO;
import com.knowledge.marketing.groupbuy.vo.TradeOrderVO;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public final class GroupBuyConverter {

    private GroupBuyConverter() {
    }

    public static GroupActivityBO toBO(GroupActivityDO source) {
        return new GroupActivityBO(source.getId(), source.getCourseId(), source.getStatus(),
                instant(source.getStartTime()), instant(source.getEndTime()), source.getTargetCount(),
                source.getMaxJoinPerUser(), source.getPriceCents(), source.getVersion());
    }

    public static GroupDetailBO toBO(GroupOrderDO source) {
        return new GroupDetailBO(source.getId(), source.getActivityId(), source.getOwnerUserId(),
                source.getStatus(), source.getTargetCount(), source.getConfirmedCount(),
                instant(source.getExpiresAt()), source.getVersion());
    }

    public static TradeOrderBO toBO(TradeOrderDO source) {
        return new TradeOrderBO(source.getId(), source.getUserId(), source.getCourseId(), source.getActivityId(),
                source.getGroupId(), source.getAmountCents(), source.getStatus(), source.getPaymentTradeNo(),
                instant(source.getCreatedAt()), instant(source.getPaidAt()));
    }

    public static GroupActivityVO toVO(GroupActivityBO source) {
        return new GroupActivityVO(source.id(), source.courseId(), source.status().name(), source.startTime(),
                source.endTime(), source.targetCount(), source.maxJoinPerUser(), source.priceCents(),
                source.version());
    }

    public static GroupDetailVO toVO(GroupDetailBO source) {
        return new GroupDetailVO(source.id(), source.activityId(), source.ownerUserId(), source.status().name(),
                source.targetCount(), source.confirmedCount(), source.expiresAt(), source.version());
    }

    public static TradeOrderVO toVO(TradeOrderBO source) {
        return new TradeOrderVO(source.orderId(), source.courseId(), source.activityId(), source.groupId(),
                source.amountCents(), source.status().name(), source.paymentTradeNo(), source.createdAt(),
                source.paidAt());
    }

    private static java.time.Instant instant(LocalDateTime value) {
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }
}
