package com.knowledge.marketing.groupbuy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.knowledge.api.common.ErrorCode;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.knowledge.common.exception.BusinessException;
import com.knowledge.marketing.groupbuy.dao.model.GroupActivityDO;
import com.knowledge.marketing.groupbuy.dao.model.GroupOrderDO;
import com.knowledge.marketing.groupbuy.dao.model.GroupParticipantDO;
import com.knowledge.marketing.groupbuy.dao.model.TradeOrderDO;
import com.knowledge.marketing.groupbuy.bo.JoinGroupBO;
import com.knowledge.marketing.groupbuy.dao.mapper.GroupActivityMapper;
import com.knowledge.marketing.groupbuy.dao.mapper.GroupOrderMapper;
import com.knowledge.marketing.groupbuy.dao.mapper.GroupParticipantMapper;
import com.knowledge.marketing.groupbuy.dao.mapper.TradeOrderMapper;
import com.knowledge.marketing.groupbuy.rule.JoinRuleChain;
import org.junit.jupiter.api.Test;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.RedisConnectionFailureException;

class GroupPurchaseServiceTest {

    @Test
    void redisFailureStopsTheWriteBeforeOpeningTheDatabaseTransaction() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "test");
        TableInfoHelper.initTableInfo(assistant, GroupParticipantDO.class);
        TableInfoHelper.initTableInfo(assistant, TradeOrderDO.class);
        GroupActivityMapper activities = mock(GroupActivityMapper.class);
        GroupOrderMapper groups = mock(GroupOrderMapper.class);
        GroupParticipantMapper participants = mock(GroupParticipantMapper.class);
        TradeOrderMapper orders = mock(TradeOrderMapper.class);
        JoinRuleChain rules = mock(JoinRuleChain.class);
        SlotReservationService slots = mock(SlotReservationService.class);
        GroupJoinTransactionService transaction = mock(GroupJoinTransactionService.class);
        JoinGroupBO request = new JoinGroupBO("g1", "u1");
        GroupActivityDO activity = new GroupActivityDO();
        GroupOrderDO group = new GroupOrderDO();
        group.setActivityId("a1");
        group.setConfirmedCount(0);
        group.setTargetCount(3);
        when(orders.selectOne(any())).thenReturn(null);
        when(activities.selectById("a1")).thenReturn(activity);
        when(groups.selectById("g1")).thenReturn(group);
        when(participants.selectCount(any())).thenReturn(0L);
        when(slots.reserve("g1", "u1", 0, 3))
                .thenThrow(new RedisConnectionFailureException("redis unavailable"));

        GroupPurchaseService service = new GroupPurchaseService(activities, groups, participants, orders,
                rules, slots, transaction);

        assertThatThrownBy(() -> service.join(request)).isInstanceOf(RedisConnectionFailureException.class);
        verify(transaction, never()).createPendingOrder(any(), any());
    }

    @Test
    void releasesFreshReservationAndReturnsOrderWonByConcurrentNaturalKeyRequest() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "test");
        TableInfoHelper.initTableInfo(assistant, GroupParticipantDO.class);
        TableInfoHelper.initTableInfo(assistant, TradeOrderDO.class);
        GroupActivityMapper activities = mock(GroupActivityMapper.class);
        GroupOrderMapper groups = mock(GroupOrderMapper.class);
        GroupParticipantMapper participants = mock(GroupParticipantMapper.class);
        TradeOrderMapper orders = mock(TradeOrderMapper.class);
        JoinRuleChain rules = mock(JoinRuleChain.class);
        SlotReservationService slots = mock(SlotReservationService.class);
        GroupJoinTransactionService transaction = mock(GroupJoinTransactionService.class);
        JoinGroupBO request = new JoinGroupBO("g1", "u1");
        GroupActivityDO activity = new GroupActivityDO();
        GroupOrderDO group = new GroupOrderDO();
        group.setActivityId("a1");
        group.setConfirmedCount(0);
        group.setTargetCount(3);
        TradeOrderDO conflicting = new TradeOrderDO();
        conflicting.setId("existing-order");
        conflicting.setUserId("u1");
        conflicting.setActivityId("a1");
        conflicting.setGroupId("g1");
        conflicting.setAmountCents(100L);
        when(orders.selectOne(any())).thenReturn(null, conflicting);
        when(activities.selectById("a1")).thenReturn(activity);
        when(groups.selectById("g1")).thenReturn(group);
        when(participants.selectCount(any())).thenReturn(0L);
        when(slots.reserve("g1", "u1", 0, 3))
                .thenReturn(SlotReservationService.ReservationResult.ACQUIRED);
        when(transaction.createPendingOrder(request, activity)).thenThrow(new DuplicateKeyException("duplicate"));

        GroupPurchaseService service = new GroupPurchaseService(activities, groups, participants, orders,
                rules, slots, transaction);

        assertThat(service.join(request).orderId()).isEqualTo("existing-order");
        verify(slots).release("g1", "u1");
    }
}
