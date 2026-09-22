package com.knowledge.marketing.notification.service;

import com.knowledge.marketing.notification.config.MarketingRabbitConfiguration;
import com.knowledge.marketing.notification.dao.model.NotificationTaskDO;
import com.knowledge.marketing.notification.enums.NotificationStatus;
import com.knowledge.marketing.notification.dao.mapper.NotificationTaskMapper;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import jakarta.annotation.Resource;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationTaskDispatcher {

    private static final int MAX_RETRY = 10;
    @Resource private NotificationTaskMapper taskMapper;
    @Resource private RabbitTemplate rabbitTemplate;
    @Resource private Clock clock;


    public int dispatchBatch(int limit) {
        int sent = 0;
        for (NotificationTaskDO task : taskMapper.findDispatchable(LocalDateTime.now(clock), limit)) {
            if (taskMapper.claim(task.getId(), LocalDateTime.now(clock)) != 1) {
                continue;
            }
            try {
                CorrelationData correlationData = new CorrelationData(task.getEventId());
                rabbitTemplate.convertAndSend(MarketingRabbitConfiguration.EVENT_EXCHANGE,
                        MarketingRabbitConfiguration.GROUP_FORMED_ROUTING_KEY, task.getPayload(), correlationData);
                CorrelationData.Confirm confirm = correlationData.getFuture().get(5, TimeUnit.SECONDS);
                if (!confirm.isAck()) {
                    throw new IllegalStateException("Broker nack: " + confirm.getReason());
                }
                markSent(task);
                sent++;
            } catch (Exception exception) {
                markRetry(task, exception);
            }
        }
        return sent;
    }

    private void markSent(NotificationTaskDO task) {
        task.setStatus(NotificationStatus.SENT);
        task.setUpdatedAt(LocalDateTime.now(clock));
        task.setLastError(null);
        taskMapper.updateById(task);
    }

    private void markRetry(NotificationTaskDO task, Exception exception) {
        int retryCount = task.getRetryCount() + 1;
        task.setRetryCount(retryCount);
        task.setStatus(retryCount >= MAX_RETRY ? NotificationStatus.DEAD : NotificationStatus.RETRY);
        long delaySeconds = Math.min(600, 1L << Math.min(retryCount, 9));
        task.setNextRetryAt(LocalDateTime.now(clock).plusSeconds(delaySeconds));
        String message = exception.getMessage();
        task.setLastError(message == null ? exception.getClass().getSimpleName()
                : message.substring(0, Math.min(message.length(), 500)));
        task.setUpdatedAt(LocalDateTime.now(clock));
        taskMapper.updateById(task);
    }
}
