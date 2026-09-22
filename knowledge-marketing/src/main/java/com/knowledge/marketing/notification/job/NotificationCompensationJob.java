package com.knowledge.marketing.notification.job;

import com.knowledge.marketing.notification.service.NotificationTaskDispatcher;
import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NotificationCompensationJob {

    @Resource private NotificationTaskDispatcher dispatcher;


    @Scheduled(fixedDelayString = "${knowledge.marketing.notification.fixed-delay:5000}")
    public void compensate() {
        dispatcher.dispatchBatch(100);
    }
}
