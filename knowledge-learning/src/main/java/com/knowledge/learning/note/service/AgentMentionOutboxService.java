package com.knowledge.learning.note.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledge.api.learning.dto.AgentMentionedEventDTO;
import com.knowledge.learning.entitlement.config.LearningRabbitConfiguration;
import com.knowledge.learning.note.dao.mapper.AgentMentionOutboxMapper;
import com.knowledge.learning.note.dao.model.AgentMentionOutboxDO;
import com.knowledge.learning.note.dao.model.NoteCommentDO;
import com.knowledge.learning.note.enums.AgentMentionOutboxStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AgentMentionOutboxService {

    private static final int MAX_RETRY = 10;

    @Autowired
    private AgentMentionOutboxMapper outboxMapper;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private Clock clock;
    @Value("${knowledge.learning.agent-mention.confirm-timeout-ms:5000}")
    private long confirmTimeoutMs;

    /** Called inside the comment creation transaction. */
    public void record(NoteCommentDO comment) {
        String eventId = UUID.randomUUID().toString();
        Instant occurredAt = Instant.now(clock);
        AgentMentionedEventDTO event = new AgentMentionedEventDTO(eventId, "AgentMentioned", occurredAt,
                comment.getId(), 1, comment.getNoteId(), comment.getId(), comment.getParentCommentId(),
                comment.getUserId());
        AgentMentionOutboxDO outbox = new AgentMentionOutboxDO();
        outbox.setId(UUID.randomUUID().toString());
        outbox.setEventId(eventId);
        outbox.setAggregateId(comment.getId());
        outbox.setEventType(event.eventType());
        outbox.setPayload(serialize(event));
        outbox.setStatus(AgentMentionOutboxStatus.PENDING);
        outbox.setRetryCount(0);
        LocalDateTime now = LocalDateTime.now(clock);
        outbox.setNextRetryAt(now);
        outbox.setCreatedAt(now);
        outbox.setUpdatedAt(now);
        outboxMapper.insert(outbox);
    }

    public int dispatchBatch(int limit) {
        if (limit < 1 || limit > 200) {
            throw new IllegalArgumentException("Outbox dispatch limit must be 1-200");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        outboxMapper.recoverStaleDispatching(now.minusMinutes(1), now);
        int sent = 0;
        for (AgentMentionOutboxDO outbox : outboxMapper.findDispatchable(now, limit)) {
            if (outboxMapper.claim(outbox.getId(), now) != 1) {
                continue;
            }
            try {
                CorrelationData correlation = new CorrelationData(outbox.getEventId());
                rabbitTemplate.convertAndSend(LearningRabbitConfiguration.EVENT_EXCHANGE,
                        LearningRabbitConfiguration.AGENT_MENTION_ROUTING_KEY, outbox.getPayload(), correlation);
                CorrelationData.Confirm confirm = correlation.getFuture().get(confirmTimeoutMs, TimeUnit.MILLISECONDS);
                if (!confirm.isAck() || correlation.getReturned() != null) {
                    throw new IllegalStateException("AgentMentioned broker delivery was not confirmed");
                }
                markSent(outbox);
                sent++;
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                markRetry(outbox, exception);
            } catch (ExecutionException | TimeoutException | RuntimeException exception) {
                markRetry(outbox, exception);
            }
        }
        return sent;
    }

    private String serialize(AgentMentionedEventDTO event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("AgentMentioned event serialization failed", exception);
        }
    }

    private void markSent(AgentMentionOutboxDO outbox) {
        outbox.setStatus(AgentMentionOutboxStatus.SENT);
        outbox.setLastError(null);
        outbox.setUpdatedAt(LocalDateTime.now(clock));
        outboxMapper.updateById(outbox);
    }

    private void markRetry(AgentMentionOutboxDO outbox, Exception exception) {
        int retryCount = outbox.getRetryCount() + 1;
        outbox.setRetryCount(retryCount);
        outbox.setStatus(retryCount >= MAX_RETRY ? AgentMentionOutboxStatus.DEAD : AgentMentionOutboxStatus.RETRY);
        outbox.setNextRetryAt(LocalDateTime.now(clock).plusSeconds(Math.min(600, 1L << Math.min(retryCount, 9))));
        String message = exception.getMessage();
        outbox.setLastError(message == null ? exception.getClass().getSimpleName()
                : message.substring(0, Math.min(message.length(), 500)));
        outbox.setUpdatedAt(LocalDateTime.now(clock));
        outboxMapper.updateById(outbox);
    }
}
