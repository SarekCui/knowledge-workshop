package com.knowledge.marketing.groupbuy.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledge.api.marketing.dto.GroupFormedEventDTO;
import com.knowledge.common.exception.BusinessException;
import com.knowledge.common.web.RequestIdSupport;
import com.knowledge.marketing.groupbuy.bo.GroupOrderBO;
import com.knowledge.marketing.groupbuy.dao.model.GroupOrderDO;
import com.knowledge.marketing.groupbuy.dao.model.TradeOrderDO;
import com.knowledge.marketing.groupbuy.enums.GroupStatus;
import com.knowledge.marketing.groupbuy.enums.TradeOrderStatus;
import com.knowledge.marketing.groupbuy.dao.mapper.GroupOrderMapper;
import com.knowledge.marketing.groupbuy.dao.mapper.GroupParticipantMapper;
import com.knowledge.marketing.groupbuy.dao.mapper.TradeOrderMapper;
import com.knowledge.marketing.notification.dao.model.NotificationTaskDO;
import com.knowledge.marketing.notification.enums.NotificationStatus;
import com.knowledge.marketing.notification.dao.mapper.NotificationTaskMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class PaymentSettlementService {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentSettlementService.class);

    private final TradeOrderMapper orderMapper;
    private final GroupParticipantMapper participantMapper;
    private final GroupOrderMapper groupMapper;
    private final NotificationTaskMapper taskMapper;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final SlotReservationService slotReservationService;

    public PaymentSettlementService(TradeOrderMapper orderMapper,
                                    GroupParticipantMapper participantMapper,
                                    GroupOrderMapper groupMapper,
                                    NotificationTaskMapper taskMapper,
                                    ObjectMapper objectMapper,
                                    Clock clock,
                                    SlotReservationService slotReservationService) {
        this.orderMapper = orderMapper;
        this.participantMapper = participantMapper;
        this.groupMapper = groupMapper;
        this.taskMapper = taskMapper;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.slotReservationService = slotReservationService;
    }

    @Transactional
    public GroupOrderBO settle(String orderId, String paymentTradeNo, String authenticatedUserId) {
        TradeOrderDO order = orderMapper.selectById(orderId);
        if (order == null) {
            throw BusinessException.notFound("订单不存在");
        }
        if (!order.getUserId().equals(authenticatedUserId)) {
            throw BusinessException.forbidden("无权操作该订单");
        }
        if (order.getStatus() == TradeOrderStatus.PAID) {
            if (!paymentTradeNo.equals(order.getPaymentTradeNo())) {
                throw BusinessException.conflict("订单已由其他支付流水完成");
            }
            confirmReservationAfterCommit(order);
            return toBusinessObject(order);
        }
        if (order.getStatus() != TradeOrderStatus.PENDING_PAYMENT) {
            throw BusinessException.conflict("当前订单状态不能支付");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        if (orderMapper.markPaid(orderId, paymentTradeNo, now) != 1) {
            throw BusinessException.conflict("订单状态已发生变化，请查询结果");
        }
        if (participantMapper.confirmByGroupAndUser(order.getGroupId(), order.getUserId(), now) != 1) {
            throw BusinessException.conflict("参团记录状态异常");
        }
        if (groupMapper.confirmOne(order.getGroupId(), now) != 1) {
            throw BusinessException.conflict("团名额已满或团已结束");
        }

        GroupOrderDO group = groupMapper.selectById(order.getGroupId());
        if (group.getStatus() == GroupStatus.FORMED) {
            createGroupFormedTask(order, group, now);
        }
        confirmReservationAfterCommit(order);
        return toBusinessObject(orderMapper.selectById(orderId));
    }

    private GroupOrderBO toBusinessObject(TradeOrderDO order) {
        return new GroupOrderBO(order.getId(), order.getGroupId(), order.getStatus(), order.getAmountCents());
    }

    private void confirmReservationAfterCommit(TradeOrderDO order) {
        Runnable confirmation = () -> {
            try {
                slotReservationService.confirm(order.getGroupId(), order.getUserId());
            } catch (RuntimeException exception) {
                // The database is already committed. The reservation compensation job will retry this cleanup.
                LOG.warn("Failed to confirm Redis reservation after payment commit, groupId={}, userId={}",
                        order.getGroupId(), order.getUserId(), exception);
            }
        };
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    confirmation.run();
                }
            });
        } else {
            confirmation.run();
        }
    }

    private void createGroupFormedTask(TradeOrderDO order, GroupOrderDO group, LocalDateTime now) {
        String eventId = "group-formed:" + group.getId();
        GroupFormedEventDTO event = new GroupFormedEventDTO(eventId, group.getId(), group.getActivityId(),
                order.getCourseId(), participantMapper.findConfirmedUserIds(group.getId()),
                RequestIdSupport.resolve(MDC.get("requestId")), Instant.now(clock), 1);
        NotificationTaskDO task = new NotificationTaskDO();
        task.setId(UUID.randomUUID().toString());
        task.setEventId(eventId);
        task.setTaskType("GROUP_FORMED");
        try {
            task.setPayload(objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("成团事件序列化失败", exception);
        }
        task.setStatus(NotificationStatus.PENDING);
        task.setRetryCount(0);
        task.setNextRetryAt(now);
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        taskMapper.insert(task);
    }
}
