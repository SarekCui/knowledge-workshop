package com.knowledge.agent.mention.job;

import com.knowledge.agent.mention.service.RunRecoveryService;
import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RunRecoveryJob {

    @Resource
    private RunRecoveryService recoveryService;

    @Scheduled(fixedDelayString = "${knowledge.agent.execution.recovery-delay:30000}")
    public void recover() {
        recoveryService.recoverBatch(50);
    }
}
