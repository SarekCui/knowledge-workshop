package com.knowledge.points.ranking.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledge.api.points.dto.PointGrantEventDTO;
import com.knowledge.points.ranking.dao.model.PointTaskDO;
import com.knowledge.points.ranking.enums.PointTaskStatus;
import com.knowledge.points.ranking.dao.mapper.PointTaskMapper;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class PointTaskService {

    private final PointTaskMapper taskMapper;
    private final ObjectMapper objectMapper;

    public PointTaskService(PointTaskMapper taskMapper, ObjectMapper objectMapper) {
        this.taskMapper = taskMapper;
        this.objectMapper = objectMapper;
    }

    public void create(PointGrantEventDTO event, LocalDateTime now) {
        PointTaskDO task = new PointTaskDO();
        task.setId(UUID.randomUUID().toString());
        task.setEventId(event.eventId());
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
