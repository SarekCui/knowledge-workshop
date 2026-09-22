package com.knowledge.marketing.groupbuy.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowledge.common.exception.BusinessException;
import com.knowledge.common.model.PageBO;
import com.knowledge.marketing.groupbuy.bo.GroupActivityBO;
import com.knowledge.marketing.groupbuy.bo.GroupDetailBO;
import com.knowledge.marketing.groupbuy.bo.TradeOrderBO;
import com.knowledge.marketing.groupbuy.converter.GroupBuyConverter;
import com.knowledge.marketing.groupbuy.dao.mapper.GroupActivityMapper;
import com.knowledge.marketing.groupbuy.dao.mapper.GroupOrderMapper;
import com.knowledge.marketing.groupbuy.dao.mapper.TradeOrderMapper;
import com.knowledge.marketing.groupbuy.dao.model.GroupActivityDO;
import com.knowledge.marketing.groupbuy.dao.model.GroupOrderDO;
import com.knowledge.marketing.groupbuy.dao.model.TradeOrderDO;
import com.knowledge.marketing.groupbuy.enums.ActivityStatus;
import com.knowledge.marketing.groupbuy.enums.GroupStatus;
import com.knowledge.marketing.groupbuy.enums.TradeOrderStatus;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.function.Function;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class GroupBuyQueryService {

    @Resource private GroupActivityMapper activityMapper;
    @Resource private GroupOrderMapper groupMapper;
    @Resource private TradeOrderMapper orderMapper;
    @Resource private Clock clock;


    public PageBO<GroupActivityBO> listAvailableActivities(int pageNo, int pageSize) {
        LocalDateTime now = LocalDateTime.now(clock);
        IPage<GroupActivityDO> page = activityMapper.selectPage(page(pageNo, pageSize),
                Wrappers.<GroupActivityDO>lambdaQuery()
                        .eq(GroupActivityDO::getStatus, ActivityStatus.ACTIVE)
                        .le(GroupActivityDO::getStartTime, now)
                        .gt(GroupActivityDO::getEndTime, now)
                        .orderByAsc(GroupActivityDO::getEndTime, GroupActivityDO::getId));
        return convert(page, GroupBuyConverter::toBO);
    }

    public GroupActivityBO getAvailableActivity(String activityId) {
        GroupActivityDO activity = requireActivity(activityId);
        LocalDateTime now = LocalDateTime.now(clock);
        if (activity.getStatus() != ActivityStatus.ACTIVE
                || now.isBefore(activity.getStartTime()) || !now.isBefore(activity.getEndTime())) {
            throw BusinessException.notFound("拼团活动不存在或当前不可参与");
        }
        return GroupBuyConverter.toBO(activity);
    }

    public PageBO<GroupDetailBO> listAvailableGroups(String activityId, int pageNo, int pageSize) {
        getAvailableActivity(activityId);
        IPage<GroupOrderDO> page = groupMapper.selectPage(page(pageNo, pageSize),
                Wrappers.<GroupOrderDO>lambdaQuery()
                        .eq(GroupOrderDO::getActivityId, activityId)
                        .eq(GroupOrderDO::getStatus, GroupStatus.FORMING)
                        .gt(GroupOrderDO::getExpiresAt, LocalDateTime.now(clock))
                        .orderByAsc(GroupOrderDO::getExpiresAt, GroupOrderDO::getId));
        return convert(page, GroupBuyConverter::toBO);
    }

    public GroupDetailBO getGroup(String groupId) {
        GroupOrderDO group = requireGroup(groupId);
        getAvailableActivity(group.getActivityId());
        return GroupBuyConverter.toBO(group);
    }

    public PageBO<TradeOrderBO> listUserOrders(String userId, TradeOrderStatus status,
                                                int pageNo, int pageSize) {
        var query = Wrappers.<TradeOrderDO>lambdaQuery().eq(TradeOrderDO::getUserId, userId);
        if (status != null) {
            query.eq(TradeOrderDO::getStatus, status);
        }
        query.orderByDesc(TradeOrderDO::getCreatedAt, TradeOrderDO::getId);
        return convert(orderMapper.selectPage(page(pageNo, pageSize), query), GroupBuyConverter::toBO);
    }

    public TradeOrderBO getUserOrder(String userId, String orderId) {
        TradeOrderDO order = orderMapper.selectById(orderId);
        if (order == null) {
            throw BusinessException.notFound("订单不存在");
        }
        if (!userId.equals(order.getUserId())) {
            throw BusinessException.forbidden("无权查看该订单");
        }
        return GroupBuyConverter.toBO(order);
    }

    public PageBO<GroupActivityBO> listActivitiesForManagement(ActivityStatus status,
                                                                int pageNo, int pageSize) {
        var query = Wrappers.<GroupActivityDO>lambdaQuery();
        if (status != null) {
            query.eq(GroupActivityDO::getStatus, status);
        }
        query.orderByDesc(GroupActivityDO::getCreatedAt, GroupActivityDO::getId);
        return convert(activityMapper.selectPage(page(pageNo, pageSize), query), GroupBuyConverter::toBO);
    }

    public GroupActivityBO getActivityForManagement(String activityId) {
        return GroupBuyConverter.toBO(requireActivity(activityId));
    }

    private GroupActivityDO requireActivity(String activityId) {
        GroupActivityDO activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw BusinessException.notFound("拼团活动不存在");
        }
        return activity;
    }

    private GroupOrderDO requireGroup(String groupId) {
        GroupOrderDO group = groupMapper.selectById(groupId);
        if (group == null) {
            throw BusinessException.notFound("团不存在");
        }
        return group;
    }

    private <T> Page<T> page(int pageNo, int pageSize) {
        return new Page<>(pageNo, pageSize);
    }

    private <S, T> PageBO<T> convert(IPage<S> page, Function<S, T> converter) {
        return new PageBO<>(page.getRecords().stream().map(converter).toList(),
                Math.toIntExact(page.getCurrent()), Math.toIntExact(page.getSize()), page.getTotal());
    }
}
