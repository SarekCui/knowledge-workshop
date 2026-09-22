package com.knowledge.marketing.groupbuy.service;

import com.knowledge.marketing.groupbuy.dao.model.GroupActivityDO;
import com.knowledge.marketing.groupbuy.dao.model.GroupParticipantDO;
import com.knowledge.marketing.groupbuy.dao.model.TradeOrderDO;
import com.knowledge.marketing.groupbuy.bo.JoinGroupBO;
import com.knowledge.marketing.groupbuy.enums.ParticipantStatus;
import com.knowledge.marketing.groupbuy.enums.TradeOrderStatus;
import com.knowledge.marketing.groupbuy.dao.mapper.GroupParticipantMapper;
import com.knowledge.marketing.groupbuy.dao.mapper.TradeOrderMapper;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GroupJoinTransactionService {

    @Resource private TradeOrderMapper tradeOrderMapper;
    @Resource private GroupParticipantMapper participantMapper;
    @Resource private Clock clock;


    @Transactional
    public TradeOrderDO createPendingOrder(JoinGroupBO request, GroupActivityDO activity) {
        LocalDateTime now = LocalDateTime.now(clock);
        GroupParticipantDO participant = new GroupParticipantDO();
        participant.setId(UUID.randomUUID().toString());
        participant.setActivityId(activity.getId());
        participant.setGroupId(request.groupId());
        participant.setUserId(request.userId());
        participant.setStatus(ParticipantStatus.RESERVED);
        participant.setCreatedAt(now);
        participant.setUpdatedAt(now);
        participantMapper.insert(participant);

        TradeOrderDO order = new TradeOrderDO();
        order.setId(UUID.randomUUID().toString());
        order.setUserId(request.userId());
        order.setCourseId(activity.getCourseId());
        order.setActivityId(activity.getId());
        order.setGroupId(request.groupId());
        order.setAmountCents(activity.getPriceCents());
        order.setStatus(TradeOrderStatus.PENDING_PAYMENT);
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        tradeOrderMapper.insert(order);
        return order;
    }
}
