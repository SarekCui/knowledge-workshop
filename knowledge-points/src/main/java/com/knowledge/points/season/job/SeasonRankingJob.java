package com.knowledge.points.season.job;

import com.knowledge.points.ranking.service.QuarterTableRouter;
import com.knowledge.points.season.service.SeasonMaintenanceService;
import com.knowledge.points.season.service.SeasonSettlementService;
import com.knowledge.points.season.rule.SeasonPeriodRule;
import jakarta.annotation.Resource;
import java.time.ZoneOffset;
import com.xxl.job.core.handler.annotation.XxlJob;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class SeasonRankingJob {
    @Resource
    private SeasonMaintenanceService maintenanceService;
    @Resource
    private QuarterTableRouter tableRouter;
    @Resource
    private Clock clock;
    @Resource
    private SeasonSettlementService settlementService;

    @XxlJob("settlePointSeason")
    public void settlePreviousSeason() {
        settlementService.settle(tableRouter.route(previousSeasonInstant()).season());
    }

    private Instant previousSeasonInstant() {
        String current = tableRouter.route(Instant.now(clock)).season();
        return SeasonPeriodRule.startsAt(current).toInstant(ZoneOffset.UTC).minusNanos(1);
    }

    @XxlJob("seasonRankingSnapshot")
    public void snapshot() {
        maintenanceService.snapshot(tableRouter.route(Instant.now(clock)).season(), 1000);
    }

    @XxlJob("archivePointLedger")
    public void archivePreviousSeason() {
        maintenanceService.archive(tableRouter.route(previousSeasonInstant()));
    }
}
