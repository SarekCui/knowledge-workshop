package com.knowledge.points.ranking.job;

import com.knowledge.points.ranking.service.PointTaskDispatcher;
import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PointTaskCompensationJob {
    @Resource private PointTaskDispatcher dispatcher;


    @Scheduled(fixedDelayString = "${knowledge.points.task.fixed-delay:5000}")
    public void compensate() {
        dispatcher.dispatchBatch(100);
    }
}
