package com.knowledge.agent.mention.service;

import com.knowledge.agent.mention.dao.mapper.AgentRunMapper;
import com.knowledge.agent.mention.dao.model.AgentRunDO;
import java.time.Clock;
import java.time.LocalDateTime;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Low-frequency recovery for state that is durable but has no active broker delivery. */
@Service
public class RunRecoveryService {

    @Resource
    private AgentRunMapper runMapper;
    @Resource
    private ExecutionOutboxService executionOutboxService;
    @Resource
    private Clock clock;

    @Transactional
    public int recoverBatch(int limit) {
        if (limit < 1 || limit > 200) {
            throw new IllegalArgumentException("Agent recovery limit must be 1-200");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        int recovered = 0;
        // TTL queues normally redeliver on time. Wait one extra minute before database recovery
        // so a healthy delayed command is not needlessly duplicated at the expiry boundary.
        for (AgentRunDO run : runMapper.findRetryDue(now.minusMinutes(1), limit)) {
            recovered += activateRetry(run, now) ? 1 : 0;
        }
        int remaining = Math.max(0, limit - recovered);
        for (AgentRunDO run : runMapper.findExpiredLease(now, remaining)) {
            recovered += recoverLease(run, now) ? 1 : 0;
        }
        return recovered;
    }

    private boolean activateRetry(AgentRunDO run, LocalDateTime now) {
        if (run.getExecutionVersion() == null || runMapper.activateRetry(run.getId(), run.getExecutionVersion(), now) != 1) {
            return false;
        }
        executionOutboxService.record(run.getId(), run.getExecutionVersion(), run.getRunType(), now);
        return true;
    }

    private boolean recoverLease(AgentRunDO run, LocalDateTime now) {
        if (run.getExecutionVersion() == null
                || runMapper.recoverExpiredLease(run.getId(), run.getExecutionVersion(), now) != 1) {
            return false;
        }
        executionOutboxService.record(run.getId(), run.getExecutionVersion(), run.getRunType(), now);
        return true;
    }
}
