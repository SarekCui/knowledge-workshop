package com.knowledge.marketing.groupbuy.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowledge.common.exception.BusinessException;
import com.knowledge.marketing.groupbuy.bo.JoinGroupBO;
import com.knowledge.marketing.groupbuy.bo.TradeOrderBO;
import com.knowledge.marketing.groupbuy.converter.GroupBuyConverter;
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
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class GroupPurchaseService {

    @Resource private GroupActivityMapper activityMapper;
    @Resource private GroupOrderMapper groupMapper;
    @Resource private GroupParticipantMapper participantMapper;
    @Resource private TradeOrderMapper tradeOrderMapper;
    @Resource private JoinRuleChain ruleChain;
    @Resource private SlotReservationService slotReservationService;
    @Resource private GroupJoinTransactionService transactionService;


    public TradeOrderBO join(JoinGroupBO request) {
        TradeOrderDO existing = findByGroupAndUser(request.groupId(), request.userId());
        if (existing != null) {
            return toBusinessObject(existing);
        }

        GroupOrderDO group = groupMapper.selectById(request.groupId());
        if (group == null) {
            throw BusinessException.notFound("团不存在");
        }
        GroupActivityDO activity = activityMapper.selectById(group.getActivityId());
        if (activity == null) {
            throw BusinessException.notFound("拼团活动不存在");
        }
        long userJoinCount = participantMapper.selectCount(new LambdaQueryWrapper<GroupParticipantDO>()
                .eq(GroupParticipantDO::getActivityId, activity.getId())
                .eq(GroupParticipantDO::getUserId, request.userId())
                .in(GroupParticipantDO::getStatus, ParticipantStatus.RESERVED, ParticipantStatus.CONFIRMED));
        ruleChain.check(new JoinValidationContext(activity, group, userJoinCount));

        SlotReservationService.ReservationResult reservation = slotReservationService.reserve(
                request.groupId(), request.userId(), group.getConfirmedCount(),
                group.getTargetCount());
        try {
            TradeOrderDO created = transactionService.createPendingOrder(request, activity);
            return toBusinessObject(created);
        } catch (DuplicateKeyException exception) {
            TradeOrderDO concurrent = findByGroupAndUser(request.groupId(), request.userId());
            if (reservation == SlotReservationService.ReservationResult.ACQUIRED) {
                slotReservationService.release(request.groupId(), request.userId());
            }
            if (concurrent != null) {
                return toBusinessObject(concurrent);
            }
            throw exception;
        } catch (RuntimeException exception) {
            if (reservation == SlotReservationService.ReservationResult.ACQUIRED) {
                slotReservationService.release(request.groupId(), request.userId());
            }
            throw exception;
        }
    }

    private TradeOrderDO findByGroupAndUser(String groupId, String userId) {
        return tradeOrderMapper.selectOne(new LambdaQueryWrapper<TradeOrderDO>()
                .eq(TradeOrderDO::getGroupId, groupId)
                .eq(TradeOrderDO::getUserId, userId));
    }

    private TradeOrderBO toBusinessObject(TradeOrderDO order) {
        return GroupBuyConverter.toBO(order);
    }
}
