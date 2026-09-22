package com.knowledge.marketing.groupbuy.job;

import com.knowledge.marketing.groupbuy.service.ReservationCompensationService;
import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReservationCompensationJob {

    @Resource private ReservationCompensationService compensationService;


    @Scheduled(fixedDelayString = "${knowledge.marketing.reservation.fixed-delay:30000}")
    public void compensate() {
        compensationService.compensate(200);
    }
}
