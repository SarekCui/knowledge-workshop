package com.knowledge.marketing.notification.job;

import com.knowledge.marketing.notification.service.NotificationTaskDispatcher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NotificationCompensationJob {

    private final NotificationTaskDispatcher dispatcher;

    public NotificationCompensationJob(NotificationTaskDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Scheduled(fixedDelayString = "${knowledge.marketing.notification.fixed-delay:5000}")
    public void compensate() {
        dispatcher.dispatchBatch(100);
    }
}
