package com.knowledge.learning.note.job;

import com.knowledge.learning.note.service.AgentMentionOutboxService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AgentMentionOutboxJob {

    @Autowired
    private AgentMentionOutboxService outboxService;

    @Scheduled(fixedDelayString = "${knowledge.learning.agent-mention.dispatch-delay:5000}")
    public void dispatch() {
        outboxService.dispatchBatch(100);
    }
}
