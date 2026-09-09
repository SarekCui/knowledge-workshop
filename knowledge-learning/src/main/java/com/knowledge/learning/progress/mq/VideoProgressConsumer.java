package com.knowledge.learning.progress.mq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledge.api.learning.dto.VideoProgressReportedEventDTO;
import com.knowledge.learning.entitlement.config.LearningRabbitConfiguration;
import com.knowledge.learning.progress.bo.VideoProgressBO;
import com.knowledge.learning.progress.service.ProgressCacheService;
import com.knowledge.learning.progress.service.ProgressTransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

@Component
public class VideoProgressConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(VideoProgressConsumer.class);

    private final ObjectMapper objectMapper;
    private final ProgressTransactionService transactionService;
    private final ProgressCacheService cacheService;

    public VideoProgressConsumer(ObjectMapper objectMapper, ProgressTransactionService transactionService,
                                 ProgressCacheService cacheService) {
        this.objectMapper = objectMapper;
        this.transactionService = transactionService;
        this.cacheService = cacheService;
    }

    @RabbitListener(queues = LearningRabbitConfiguration.PROGRESS_QUEUE)
    public void consume(String payload) {
        VideoProgressReportedEventDTO event;
        try {
            event = objectMapper.readValue(payload, VideoProgressReportedEventDTO.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("无法解析学习进度事件", exception);
        }
        VideoProgressBO progress = transactionService.process(event);
        try {
            cacheService.put(event.userId(), progress);
        } catch (DataAccessException cacheFailure) {
            LOGGER.warn("Progress persisted but Redis projection failed, eventId={}", event.eventId(), cacheFailure);
        }
    }
}
