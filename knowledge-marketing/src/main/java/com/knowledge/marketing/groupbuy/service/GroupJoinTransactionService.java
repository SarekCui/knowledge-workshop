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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GroupJoinTransactionService {

    private final TradeOrderMapper tradeOrderMapper;
    private final GroupParticipantMapper participantMapper;
    private final Clock clock;

    public GroupJoinTransactionService(TradeOrderMapper tradeOrderMapper,
                                       GroupParticipantMapper participantMapper,
                                       Clock clock) {
        this.tradeOrderMapper = tradeOrderMapper;
        this.participantMapper = participantMapper;
        this.clock = clock;
    }

    @Transactional
    public TradeOrderDO createPendingOrder(JoinGroupBO request, GroupActivityDO activity) {
        LocalDateTime now = LocalDateTime.now(clock);
        GroupParticipantDO participant = new GroupParticipantDO();
        participant.setId(UUID.randomUUID().toString());
        participant.setActivityId(request.activityId());
        participant.setGroupId(request.groupId());
        participant.setUserId(request.userId());
        participant.setRequestId(request.requestId());
        participant.setStatus(ParticipantStatus.RESERVED);
        participant.setCreatedAt(now);
        participant.setUpdatedAt(now);
        participantMapper.insert(participant);

        TradeOrderDO order = new TradeOrderDO();
        order.setId(UUID.randomUUID().toString());
        order.setBusinessRequestId(request.requestId());
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
