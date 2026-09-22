package com.knowledge.agent.mention.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledge.agent.mention.config.MessagingConfiguration;
import com.knowledge.agent.mention.dao.mapper.AgentExecutionOutboxMapper;
import com.knowledge.agent.mention.dao.model.AgentExecutionOutboxDO;
import com.knowledge.agent.mention.enums.ExecutionOutboxStatus;
import com.knowledge.agent.mention.mq.ExecuteRunCommand;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ExecutionOutboxService {

    private static final int MAX_RETRY = 10;

    @Resource
    private AgentExecutionOutboxMapper outboxMapper;
    @Resource
    private ObjectMapper objectMapper;
    @Resource
    private RabbitTemplate rabbitTemplate;
    @Resource
    private Clock clock;
    @Value("${knowledge.agent.execution.confirm-timeout-ms:5000}")
    private long confirmTimeoutMs;

    /** Must be invoked inside the transaction that creates or reactivates the run. */
    public void record(String runId, long executionVersion, String runType, LocalDateTime now) {
        record(runId, executionVersion, runType, MessagingConfiguration.AGENT_RUN_REQUESTED_ROUTING_KEY, now);
    }

    /** Writes a command to either the immediate execution route or a TTL retry route. */
    public void record(String runId, long executionVersion, String runType, String routingKey, LocalDateTime now) {
        ExecuteRunCommand message = new ExecuteRunCommand(UUID.randomUUID().toString(),
                Instant.now(clock), runId, runType);
        AgentExecutionOutboxDO outbox = new AgentExecutionOutboxDO();
        outbox.setId(UUID.randomUUID().toString());
        outbox.setRunId(runId);
        outbox.setExecutionVersion(executionVersion);
        outbox.setRoutingKey(routingKey);
        outbox.setPayload(serialize(message));
        outbox.setStatus(ExecutionOutboxStatus.PENDING);
        outbox.setRetryCount(0);
        outbox.setNextRetryAt(now);
        outbox.setCreatedAt(now);
        outbox.setUpdatedAt(now);
        outboxMapper.insert(outbox);
    }

    public int dispatchBatch(int limit) {
        if (limit < 1 || limit > 200) {
            throw new IllegalArgumentException("Agent execution Outbox dispatch limit must be 1-200");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        outboxMapper.recoverStaleSending(now.minusMinutes(1), now);
        int sent = 0;
        for (AgentExecutionOutboxDO outbox : outboxMapper.findDispatchable(now, limit)) {
            if (outboxMapper.claim(outbox.getId(), now) != 1) {
                continue;
            }
            try {
                CorrelationData correlation = new CorrelationData(outbox.getId());
                rabbitTemplate.convertAndSend(MessagingConfiguration.EXECUTION_EXCHANGE,
                        outbox.getRoutingKey(), outbox.getPayload(), correlation);
                CorrelationData.Confirm confirm = correlation.getFuture().get(confirmTimeoutMs, TimeUnit.MILLISECONDS);
                if (!confirm.isAck() || correlation.getReturned() != null) {
                    throw new IllegalStateException("ExecuteRun broker delivery was not confirmed");
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

    private String serialize(ExecuteRunCommand message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("ExecuteRun command serialization failed", exception);
        }
    }

    private void markSent(AgentExecutionOutboxDO outbox) {
        outbox.setStatus(ExecutionOutboxStatus.SENT);
        outbox.setLastError(null);
        outbox.setUpdatedAt(LocalDateTime.now(clock));
        outboxMapper.updateById(outbox);
    }

    private void markRetry(AgentExecutionOutboxDO outbox, Exception exception) {
        int retryCount = outbox.getRetryCount() + 1;
        outbox.setRetryCount(retryCount);
        outbox.setStatus(retryCount >= MAX_RETRY ? ExecutionOutboxStatus.DEAD : ExecutionOutboxStatus.RETRY);
        outbox.setNextRetryAt(LocalDateTime.now(clock).plusSeconds(Math.min(600, 1L << Math.min(retryCount, 9))));
        String message = exception.getMessage();
        outbox.setLastError(message == null ? exception.getClass().getSimpleName()
                : message.substring(0, Math.min(message.length(), 500)));
        outbox.setUpdatedAt(LocalDateTime.now(clock));
        outboxMapper.updateById(outbox);
    }
}
