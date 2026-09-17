package com.knowledge.points.ranking.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledge.api.points.dto.PointGrantEventDTO;
import com.knowledge.points.ranking.dao.model.PointTaskDO;
import com.knowledge.points.ranking.enums.PointTaskStatus;
import com.knowledge.points.ranking.dao.mapper.PointTaskMapper;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.knowledge.points.season.service.SeasonWriteService;

@Service
public class PointTaskService {

    @Autowired
    private PointTaskMapper taskMapper;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private SeasonWriteService seasonWriteService;
    @Autowired
    private QuarterTableRouter tableRouter;

    @Transactional(propagation = Propagation.MANDATORY)
    public void create(PointGrantEventDTO event, LocalDateTime now) {
        seasonWriteService.requireWritable(tableRouter.route(event.occurredAt()).season());
        PointTaskDO task = new PointTaskDO();
        task.setId(UUID.randomUUID().toString());
        task.setEventId(event.eventId());
        task.setOccurredAt(LocalDateTime.ofInstant(event.occurredAt(), ZoneOffset.UTC));
        try {
            task.setPayload(objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("积分事件序列化失败", exception);
        }
        task.setStatus(PointTaskStatus.PENDING);
        task.setRetryCount(0);
        task.setNextRetryAt(now);
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        taskMapper.insert(task);
    }
}
