package com.knowledge.marketing.groupbuy.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowledge.common.exception.BusinessException;
import com.knowledge.marketing.groupbuy.bo.JoinGroupBO;
import com.knowledge.marketing.groupbuy.bo.GroupOrderBO;
import com.knowledge.marketing.groupbuy.dao.model.GroupActivityDO;
import com.knowledge.marketing.groupbuy.dao.model.GroupOrderDO;
import com.knowledge.marketing.groupbuy.dao.model.GroupParticipantDO;
import com.knowledge.marketing.groupbuy.dao.model.TradeOrderDO;
import com.knowledge.marketing.groupbuy.enums.ParticipantStatus;
import com.knowledge.marketing.groupbuy.dao.mapper.GroupActivityMapper;
import com.knowledge.marketing.groupbuy.dao.mapper.GroupOrderMapper;
import com.knowledge.marketing.groupbuy.dao.mapper.GroupParticipantMapper;
import com.knowledge.marketing.groupbuy.dao.mapper.TradeOrderMapper;
import com.knowledge.marketing.groupbuy.rule.JoinRuleChain;
import com.knowledge.marketing.groupbuy.rule.JoinValidationContext;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class GroupPurchaseService {

    private final GroupActivityMapper activityMapper;
    private final GroupOrderMapper groupMapper;
    private final GroupParticipantMapper participantMapper;
    private final TradeOrderMapper tradeOrderMapper;
    private final JoinRuleChain ruleChain;
    private final SlotReservationService slotReservationService;
    private final GroupJoinTransactionService transactionService;

    public GroupPurchaseService(GroupActivityMapper activityMapper,
                                GroupOrderMapper groupMapper,
                                GroupParticipantMapper participantMapper,
                                TradeOrderMapper tradeOrderMapper,
                                JoinRuleChain ruleChain,
                                SlotReservationService slotReservationService,
                                GroupJoinTransactionService transactionService) {
        this.activityMapper = activityMapper;
        this.groupMapper = groupMapper;
        this.participantMapper = participantMapper;
        this.tradeOrderMapper = tradeOrderMapper;
        this.ruleChain = ruleChain;
        this.slotReservationService = slotReservationService;
        this.transactionService = transactionService;
    }

    public GroupOrderBO join(JoinGroupBO request) {
        TradeOrderDO existing = findByRequestId(request.requestId());
        if (existing != null) {
            verifyIdempotentRequest(existing, request);
            return toBusinessObject(existing);
        }

        GroupActivityDO activity = activityMapper.selectById(request.activityId());
        GroupOrderDO group = groupMapper.selectById(request.groupId());
        if (activity == null || group == null || !request.activityId().equals(group.getActivityId())) {
            throw BusinessException.notFound("拼团活动或团不存在");
        }
        long userJoinCount = participantMapper.selectCount(new LambdaQueryWrapper<GroupParticipantDO>()
                .eq(GroupParticipantDO::getActivityId, request.activityId())
                .eq(GroupParticipantDO::getUserId, request.userId())
                .in(GroupParticipantDO::getStatus, ParticipantStatus.RESERVED, ParticipantStatus.CONFIRMED));
        ruleChain.check(new JoinValidationContext(request, activity, group, userJoinCount));

        SlotReservationService.ReservationResult reservation = slotReservationService.reserve(
                request.groupId(), request.requestId(), request.userId(), group.getConfirmedCount(),
                group.getTargetCount());
        try {
            TradeOrderDO created = transactionService.createPendingOrder(request, activity);
            return toBusinessObject(created);
        } catch (DuplicateKeyException exception) {
            TradeOrderDO concurrent = findByRequestId(request.requestId());
            if (reservation == SlotReservationService.ReservationResult.ACQUIRED) {
                slotReservationService.release(request.groupId(), request.requestId(), request.userId());
            }
            if (concurrent != null) {
                verifyIdempotentRequest(concurrent, request);
                return toBusinessObject(concurrent);
            }
            throw exception;
        } catch (RuntimeException exception) {
            if (reservation == SlotReservationService.ReservationResult.ACQUIRED) {
                slotReservationService.release(request.groupId(), request.requestId(), request.userId());
            }
            throw exception;
        }
    }

    private TradeOrderDO findByRequestId(String requestId) {
        return tradeOrderMapper.selectOne(new LambdaQueryWrapper<TradeOrderDO>()
                .eq(TradeOrderDO::getBusinessRequestId, requestId));
    }

    private GroupOrderBO toBusinessObject(TradeOrderDO order) {
        return new GroupOrderBO(order.getId(), order.getGroupId(), order.getStatus(), order.getAmountCents());
    }

    private void verifyIdempotentRequest(TradeOrderDO existing, JoinGroupBO request) {
        if (!existing.getUserId().equals(request.userId())
                || !existing.getActivityId().equals(request.activityId())
                || !existing.getGroupId().equals(request.groupId())) {
            throw BusinessException.conflict("请求幂等键对应的业务参数不一致");
        }
    }
}
