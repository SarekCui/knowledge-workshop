package com.knowledge.agent.mention.service;

import com.knowledge.agent.mention.config.MessagingConfiguration;
import com.knowledge.agent.mention.dao.mapper.AgentRunMapper;
import java.time.Clock;
import java.time.LocalDateTime;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Persists a retry checkpoint and its delayed command in one local transaction. */
@Service
public class RetryService {

    @Resource
    private AgentRunMapper runMapper;
    @Resource
    private ExecutionOutboxService executionOutboxService;
    @Resource
    private Clock clock;
    @Value("${knowledge.agent.execution.max-attempts:5}")
    private int maxAttempts;

    @Transactional
    public void schedule(String runId, long executionVersion, String runType, int attempt,
            String errorCode, String errorSummary) {
        LocalDateTime now = LocalDateTime.now(clock);
        if (attempt >= Math.max(1, maxAttempts)) {
            runMapper.markDead(runId, executionVersion, errorCode, errorSummary, now);
            return;
        }
        RetryPlan plan = retryPlan(attempt);
        if (runMapper.scheduleRetry(runId, executionVersion, now.plusSeconds(plan.delaySeconds()), errorCode,
                errorSummary, now) == 1) {
            executionOutboxService.record(runId, executionVersion, runType, plan.routingKey(), now);
        }
    }

    private RetryPlan retryPlan(int attempt) {
        if (attempt <= 1) {
            return new RetryPlan(10, MessagingConfiguration.AGENT_RUN_RETRY_10S_ROUTING_KEY);
        }
        if (attempt <= 3) {
            return new RetryPlan(60, MessagingConfiguration.AGENT_RUN_RETRY_60S_ROUTING_KEY);
        }
        return new RetryPlan(300, MessagingConfiguration.AGENT_RUN_RETRY_300S_ROUTING_KEY);
    }

    private record RetryPlan(long delaySeconds, String routingKey) {
    }
}
