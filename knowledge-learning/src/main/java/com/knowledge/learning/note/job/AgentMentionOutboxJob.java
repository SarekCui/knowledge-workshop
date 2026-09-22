package com.knowledge.learning.note.job;

import com.knowledge.learning.note.service.AgentMentionOutboxService;
import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AgentMentionOutboxJob {

    @Resource
    private AgentMentionOutboxService outboxService;

    @Scheduled(fixedDelayString = "${knowledge.learning.agent-mention.dispatch-delay:5000}")
    public void dispatch() {
        outboxService.dispatchBatch(100);
    }
}
