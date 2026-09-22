package com.knowledge.learning.entitlement.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.knowledge.api.marketing.dto.GroupFormedEventDTO;
import com.knowledge.common.exception.BusinessException;
import com.knowledge.learning.entitlement.bo.CourseEntitlementBO;
import com.knowledge.learning.entitlement.converter.EntitlementConverter;
import com.knowledge.learning.entitlement.dao.mapper.CourseEntitlementMapper;
import com.knowledge.learning.entitlement.dao.mapper.MessageInboxMapper;
import com.knowledge.learning.entitlement.dao.model.CourseEntitlementDO;
import com.knowledge.learning.entitlement.dao.model.MessageInboxDO;
import com.knowledge.learning.entitlement.enums.EntitlementStatus;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EntitlementService {

    static final String CONSUMER_NAME = "learning-entitlement-v1";

    @Resource private CourseEntitlementMapper entitlementMapper;
    @Resource private MessageInboxMapper inboxMapper;
    @Resource private Clock clock;


    @Transactional
    public void grantFromGroup(GroupFormedEventDTO event) {
        if (event.eventId() == null || event.courseId() == null || event.groupId() == null
                || event.userIds() == null || event.userIds().isEmpty()) {
            throw new IllegalArgumentException("成团事件缺少权益发放字段");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        MessageInboxDO inbox = new MessageInboxDO();
        inbox.setId(UUID.randomUUID().toString());
        inbox.setConsumerName(CONSUMER_NAME);
        inbox.setEventId(event.eventId());
        inbox.setProcessedAt(now);
        if (inboxMapper.insertIgnore(inbox) == 0) {
            return;
        }
        for (String userId : event.userIds().stream().distinct().toList()) {
            if (userId == null || userId.isBlank()) {
                throw new IllegalArgumentException("成团事件包含空用户 ID");
            }
            CourseEntitlementDO entitlement = new CourseEntitlementDO();
            entitlement.setId(UUID.randomUUID().toString());
            entitlement.setUserId(userId);
            entitlement.setCourseId(event.courseId());
            entitlement.setSourceType("GROUP_BUY");
            entitlement.setSourceId(event.groupId());
            entitlement.setStatus(EntitlementStatus.ACTIVE);
            entitlement.setEffectiveAt(now);
            entitlement.setExpiresAt(null);
            entitlement.setCreatedAt(now);
            entitlement.setUpdatedAt(now);
            entitlementMapper.insertIgnore(entitlement);
        }
    }

    public boolean hasActive(String userId, String courseId) {
        LocalDateTime now = LocalDateTime.now(clock);
        return entitlementMapper.selectCount(Wrappers.<CourseEntitlementDO>lambdaQuery()
                .eq(CourseEntitlementDO::getUserId, userId)
                .eq(CourseEntitlementDO::getCourseId, courseId)
                .eq(CourseEntitlementDO::getStatus, EntitlementStatus.ACTIVE)
                .le(CourseEntitlementDO::getEffectiveAt, now)
                .and(query -> query.isNull(CourseEntitlementDO::getExpiresAt)
                        .or().gt(CourseEntitlementDO::getExpiresAt, now))) > 0;
    }

    public void requireActive(String userId, String courseId) {
        if (!hasActive(userId, courseId)) {
            throw BusinessException.forbidden("尚未获得该课程的学习权益");
        }
    }

    public List<CourseEntitlementBO> listActive(String userId) {
        LocalDateTime now = LocalDateTime.now(clock);
        return entitlementMapper.selectList(Wrappers.<CourseEntitlementDO>lambdaQuery()
                        .eq(CourseEntitlementDO::getUserId, userId)
                        .eq(CourseEntitlementDO::getStatus, EntitlementStatus.ACTIVE)
                        .le(CourseEntitlementDO::getEffectiveAt, now)
                        .and(query -> query.isNull(CourseEntitlementDO::getExpiresAt)
                                .or().gt(CourseEntitlementDO::getExpiresAt, now))
                        .orderByDesc(CourseEntitlementDO::getEffectiveAt)
                        .last("LIMIT 100"))
                .stream().map(EntitlementConverter::toBO).toList();
    }
}
