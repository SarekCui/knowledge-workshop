package com.knowledge.marketing.notification.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowledge.common.exception.BusinessException;
import com.knowledge.common.model.PageBO;
import com.knowledge.marketing.notification.bo.NotificationTaskBO;
import com.knowledge.marketing.notification.converter.NotificationTaskConverter;
import com.knowledge.marketing.notification.dao.mapper.NotificationTaskMapper;
import com.knowledge.marketing.notification.dao.model.NotificationTaskDO;
import com.knowledge.marketing.notification.enums.NotificationStatus;
import java.time.Clock;
import java.time.LocalDateTime;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationTaskManagementService {

    @Resource private NotificationTaskMapper taskMapper;
    @Resource private Clock clock;


    public PageBO<NotificationTaskBO> list(NotificationStatus status, int pageNo, int pageSize) {
        var query = Wrappers.<NotificationTaskDO>lambdaQuery();
        if (status != null) {
            query.eq(NotificationTaskDO::getStatus, status);
        }
        query.orderByDesc(NotificationTaskDO::getCreatedAt, NotificationTaskDO::getId);
        IPage<NotificationTaskDO> page = taskMapper.selectPage(new Page<>(pageNo, pageSize), query);
        return new PageBO<>(page.getRecords().stream().map(NotificationTaskConverter::toBO).toList(),
                Math.toIntExact(page.getCurrent()), Math.toIntExact(page.getSize()), page.getTotal());
    }

    @Transactional
    public NotificationTaskBO retry(String taskId) {
        NotificationTaskDO task = taskMapper.selectById(taskId);
        if (task == null) {
            throw BusinessException.notFound("通知任务不存在");
        }
        if (task.getStatus() != NotificationStatus.RETRY && task.getStatus() != NotificationStatus.DEAD) {
            throw BusinessException.conflict("只有重试中或死信状态的通知任务可以人工重试");
        }
        if (taskMapper.resetForManualRetry(taskId, LocalDateTime.now(clock)) != 1) {
            throw BusinessException.conflict("通知任务状态已发生变化，请刷新后重试");
        }
        return NotificationTaskConverter.toBO(taskMapper.selectById(taskId));
    }
}
