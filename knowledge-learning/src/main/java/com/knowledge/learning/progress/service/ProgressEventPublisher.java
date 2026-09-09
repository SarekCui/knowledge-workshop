package com.knowledge.learning.progress.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledge.api.learning.dto.VideoProgressReportedEventDTO;
import com.knowledge.common.exception.BusinessException;
import com.knowledge.learning.entitlement.config.LearningRabbitConfiguration;
import java.util.concurrent.TimeUnit;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class ProgressEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public ProgressEventPublisher(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    public void publish(VideoProgressReportedEventDTO event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            CorrelationData correlation = new CorrelationData(event.eventId());
            rabbitTemplate.convertAndSend(LearningRabbitConfiguration.EVENT_EXCHANGE,
                    LearningRabbitConfiguration.PROGRESS_ROUTING_KEY, payload, correlation);
            CorrelationData.Confirm confirm = correlation.getFuture().get(5, TimeUnit.SECONDS);
            if (!confirm.isAck()) {
                throw BusinessException.serviceUnavailable("学习进度暂时无法可靠保存，请稍后重试");
            }
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("学习进度事件序列化失败", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw BusinessException.serviceUnavailable("学习进度保存被中断，请稍后重试");
        } catch (java.util.concurrent.ExecutionException | java.util.concurrent.TimeoutException exception) {
            throw BusinessException.serviceUnavailable("学习进度暂时无法可靠保存，请稍后重试");
        }
    }
}
