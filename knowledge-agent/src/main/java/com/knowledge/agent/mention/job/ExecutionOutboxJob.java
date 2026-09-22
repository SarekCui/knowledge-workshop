package com.knowledge.agent.mention.job;

import com.knowledge.agent.mention.service.ExecutionOutboxService;
import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ExecutionOutboxJob {

    @Resource
    private ExecutionOutboxService executionOutboxService;

    @Scheduled(fixedDelayString = "${knowledge.agent.execution.outbox-dispatch-delay:1000}")
    public void dispatch() {
        executionOutboxService.dispatchBatch(50);
    }
}
