package com.knowledge.learning.progress.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledge.api.learning.dto.VideoProgressReportedEventDTO;
import com.knowledge.common.exception.BusinessException;
import com.knowledge.learning.entitlement.config.LearningRabbitConfiguration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.amqp.AmqpException;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;

@Service
public class ProgressEventPublisher {

    @Resource
    private RabbitTemplate rabbitTemplate;
    @Resource
    private ObjectMapper objectMapper;
    @Value("${knowledge.learning.progress.confirm-timeout-ms:5000}")
    private long confirmTimeoutMs;

    public void publish(VideoProgressReportedEventDTO event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            CorrelationData correlation = new CorrelationData(event.eventId());
            rabbitTemplate.convertAndSend(LearningRabbitConfiguration.EVENT_EXCHANGE,
                    LearningRabbitConfiguration.PROGRESS_ROUTING_KEY, payload, correlation);
            CorrelationData.Confirm confirm = correlation.getFuture().get(confirmTimeoutMs, TimeUnit.MILLISECONDS);
            if (!confirm.isAck() || correlation.getReturned() != null) {
                throw BusinessException.serviceUnavailable("学习进度暂时无法可靠保存，请稍后重试");
            }
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("学习进度事件序列化失败", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw BusinessException.serviceUnavailable("学习进度保存被中断，请稍后重试");
        } catch (AmqpException | ExecutionException | TimeoutException exception) {
            throw BusinessException.serviceUnavailable("学习进度暂时无法可靠保存，请稍后重试");
        }
    }
}
