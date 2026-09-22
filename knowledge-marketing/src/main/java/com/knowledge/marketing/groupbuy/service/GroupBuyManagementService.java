package com.knowledge.marketing.groupbuy.service;

import com.knowledge.common.exception.BusinessException;
import com.knowledge.marketing.groupbuy.bo.GroupActivityBO;
import com.knowledge.marketing.groupbuy.bo.GroupDetailBO;
import com.knowledge.marketing.groupbuy.converter.GroupBuyConverter;
import com.knowledge.marketing.groupbuy.dao.mapper.GroupActivityMapper;
import com.knowledge.marketing.groupbuy.dao.mapper.GroupOrderMapper;
import com.knowledge.marketing.groupbuy.dao.model.GroupActivityDO;
import com.knowledge.marketing.groupbuy.dao.model.GroupOrderDO;
import com.knowledge.marketing.groupbuy.dto.ChangeGroupActivityStatusDTO;
import com.knowledge.marketing.groupbuy.dto.CreateGroupActivityDTO;
import com.knowledge.marketing.groupbuy.dto.CreateGroupDTO;
import com.knowledge.marketing.groupbuy.dto.UpdateGroupActivityDTO;
import com.knowledge.marketing.groupbuy.enums.ActivityStatus;
import com.knowledge.marketing.groupbuy.enums.GroupStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GroupBuyManagementService {

    @Resource private GroupActivityMapper activityMapper;
    @Resource private GroupOrderMapper groupMapper;
    @Resource private GroupBuyQueryService queryService;
    @Resource private Clock clock;


    @Transactional
    public GroupActivityBO createActivity(CreateGroupActivityDTO request) {
        validateWindow(request.startTime(), request.endTime());
        GroupActivityDO existing = activityMapper.selectById(request.activityId());
        if (existing != null) {
            verifySameActivity(existing, request);
            return GroupBuyConverter.toBO(existing);
        }
        LocalDateTime now = LocalDateTime.now(clock);
        GroupActivityDO activity = new GroupActivityDO();
        activity.setId(request.activityId());
        activity.setCourseId(request.courseId().trim());
        activity.setStatus(ActivityStatus.DRAFT);
        activity.setStartTime(utc(request.startTime()));
        activity.setEndTime(utc(request.endTime()));
        activity.setTargetCount(request.targetCount());
        activity.setMaxJoinPerUser(request.maxJoinPerUser());
        activity.setPriceCents(request.priceCents());
        activity.setVersion(0);
        activity.setCreatedAt(now);
        activity.setUpdatedAt(now);
        try {
            activityMapper.insert(activity);
            return GroupBuyConverter.toBO(activity);
        } catch (DuplicateKeyException exception) {
            GroupActivityDO concurrent = activityMapper.selectById(request.activityId());
            if (concurrent == null) {
                throw exception;
            }
            verifySameActivity(concurrent, request);
            return GroupBuyConverter.toBO(concurrent);
        }
    }

    @Transactional
    public GroupActivityBO updateActivity(String activityId, UpdateGroupActivityDTO request) {
        validateWindow(request.startTime(), request.endTime());
        GroupActivityBO current = queryService.getActivityForManagement(activityId);
        if (current.status() != ActivityStatus.DRAFT) {
            throw BusinessException.conflict("只有草稿活动可以修改配置");
        }
        int changed = activityMapper.updateDraft(activityId, request.courseId().trim(),
                utc(request.startTime()), utc(request.endTime()), request.targetCount(), request.maxJoinPerUser(),
                request.priceCents(), request.version(), LocalDateTime.now(clock));
        if (changed != 1) {
            throw BusinessException.conflict("活动已被其他请求修改，请刷新后重试");
        }
        return queryService.getActivityForManagement(activityId);
    }

    @Transactional
    public GroupActivityBO changeStatus(String activityId, ChangeGroupActivityStatusDTO request) {
        GroupActivityBO current = queryService.getActivityForManagement(activityId);
        if (current.status() == request.status()) {
            return current;
        }
        if (!allowed(current.status(), request.status())) {
            throw BusinessException.conflict("活动状态不能从 " + current.status() + " 变更为 " + request.status());
        }
        if (request.status() == ActivityStatus.ACTIVE && !Instant.now(clock).isBefore(current.endTime())) {
            throw BusinessException.conflict("活动已过结束时间，不能开启");
        }
        int changed = activityMapper.changeStatus(activityId, current.status().name(), request.status().name(),
                request.version(), LocalDateTime.now(clock));
        if (changed != 1) {
            throw BusinessException.conflict("活动已被其他请求修改，请刷新后重试");
        }
        return queryService.getActivityForManagement(activityId);
    }

    @Transactional
    public GroupDetailBO createGroup(String activityId, CreateGroupDTO request) {
        GroupOrderDO existing = groupMapper.selectById(request.groupId());
        if (existing != null) {
            verifySameGroup(existing, activityId, request);
            return GroupBuyConverter.toBO(existing);
        }
        GroupActivityBO activity = queryService.getActivityForManagement(activityId);
        Instant now = Instant.now(clock);
        if (activity.status() != ActivityStatus.ACTIVE
                || now.isBefore(activity.startTime()) || !now.isBefore(activity.endTime())) {
            throw BusinessException.conflict("只有有效期内已开启的活动可以创建团");
        }
        Instant expiresAt = millis(request.expiresAt());
        if (!expiresAt.isAfter(now) || expiresAt.isAfter(activity.endTime())) {
            throw BusinessException.badRequest("团过期时间必须晚于当前时间且不超过活动结束时间");
        }
        LocalDateTime timestamp = LocalDateTime.now(clock);
        GroupOrderDO group = new GroupOrderDO();
        group.setId(request.groupId());
        group.setActivityId(activityId);
        group.setOwnerUserId(request.ownerUserId().trim());
        group.setStatus(GroupStatus.FORMING);
        group.setTargetCount(activity.targetCount());
        group.setConfirmedCount(0);
        group.setExpiresAt(utc(expiresAt));
        group.setVersion(0);
        group.setCreatedAt(timestamp);
        group.setUpdatedAt(timestamp);
        try {
            groupMapper.insert(group);
            return GroupBuyConverter.toBO(group);
        } catch (DuplicateKeyException exception) {
            GroupOrderDO concurrent = groupMapper.selectById(request.groupId());
            if (concurrent == null) {
                throw exception;
            }
            verifySameGroup(concurrent, activityId, request);
            return GroupBuyConverter.toBO(concurrent);
        }
    }

    private boolean allowed(ActivityStatus current, ActivityStatus next) {
        return (current == ActivityStatus.DRAFT
                && (next == ActivityStatus.ACTIVE || next == ActivityStatus.CLOSED))
                || (current == ActivityStatus.ACTIVE && next == ActivityStatus.CLOSED);
    }

    private void validateWindow(Instant startTime, Instant endTime) {
        if (!millis(startTime).isBefore(millis(endTime))) {
            throw BusinessException.badRequest("活动开始时间必须早于结束时间");
        }
    }

    private void verifySameActivity(GroupActivityDO existing, CreateGroupActivityDTO request) {
        boolean same = existing.getCourseId().equals(request.courseId().trim())
                && existing.getStartTime().equals(utc(request.startTime()))
                && existing.getEndTime().equals(utc(request.endTime()))
                && existing.getTargetCount().equals(request.targetCount())
                && existing.getMaxJoinPerUser().equals(request.maxJoinPerUser())
                && existing.getPriceCents().equals(request.priceCents());
        if (!same) {
            throw BusinessException.conflict("activityId 已绑定其他活动参数");
        }
    }

    private void verifySameGroup(GroupOrderDO existing, String activityId, CreateGroupDTO request) {
        boolean same = existing.getActivityId().equals(activityId)
                && existing.getOwnerUserId().equals(request.ownerUserId().trim())
                && existing.getExpiresAt().equals(utc(request.expiresAt()));
        if (!same) {
            throw BusinessException.conflict("groupId 已绑定其他团参数");
        }
    }

    private LocalDateTime utc(Instant value) {
        return LocalDateTime.ofInstant(millis(value), ZoneOffset.UTC);
    }

    private Instant millis(Instant value) {
        return value.truncatedTo(ChronoUnit.MILLIS);
    }
}
