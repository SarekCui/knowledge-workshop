package com.knowledge.points.ranking.service;

import com.knowledge.points.ranking.config.PointsRabbitConfiguration;
import com.knowledge.points.ranking.dao.model.PointTaskDO;
import com.knowledge.points.ranking.enums.PointTaskStatus;
import com.knowledge.points.ranking.dao.mapper.PointTaskMapper;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class PointTaskDispatcher {

    private static final int MAX_RETRY = 10;
    private final PointTaskMapper taskMapper;
    private final RabbitTemplate rabbitTemplate;
    private final Clock clock;

    public PointTaskDispatcher(PointTaskMapper taskMapper, RabbitTemplate rabbitTemplate, Clock clock) {
        this.taskMapper = taskMapper;
        this.rabbitTemplate = rabbitTemplate;
        this.clock = clock;
    }

    public int dispatchBatch(int limit) {
        int sent = 0;
        for (PointTaskDO task : taskMapper.findDispatchable(LocalDateTime.now(clock), limit)) {
            if (taskMapper.claim(task.getId(), LocalDateTime.now(clock)) != 1) {
                continue;
            }
            try {
                CorrelationData correlation = new CorrelationData(task.getEventId());
                rabbitTemplate.convertAndSend(PointsRabbitConfiguration.EVENT_EXCHANGE,
                        PointsRabbitConfiguration.POINT_GRANT_ROUTING_KEY, task.getPayload(), correlation);
                CorrelationData.Confirm confirm = correlation.getFuture().get(5, TimeUnit.SECONDS);
                if (!confirm.isAck()) {
                    throw new IllegalStateException("Broker nack: " + confirm.getReason());
                }
                task.setStatus(PointTaskStatus.SENT);
                task.setLastError(null);
                task.setUpdatedAt(LocalDateTime.now(clock));
                taskMapper.updateById(task);
                sent++;
            } catch (Exception exception) {
                boolean interrupted = exception instanceof InterruptedException;
                if (interrupted) {
                    Thread.currentThread().interrupt();
                }
                retry(task, exception);
                if (interrupted) {
                    break;
                }
            }
        }
        return sent;
    }

    private void retry(PointTaskDO task, Exception exception) {
        int count = task.getRetryCount() + 1;
        task.setRetryCount(count);
        task.setStatus(count >= MAX_RETRY ? PointTaskStatus.DEAD : PointTaskStatus.RETRY);
        task.setNextRetryAt(LocalDateTime.now(clock).plusSeconds(Math.min(600, 1L << Math.min(count, 9))));
        String message = exception.getMessage();
        task.setLastError(message == null ? exception.getClass().getSimpleName()
                : message.substring(0, Math.min(message.length(), 500)));
        task.setUpdatedAt(LocalDateTime.now(clock));
        taskMapper.updateById(task);
    }
}
