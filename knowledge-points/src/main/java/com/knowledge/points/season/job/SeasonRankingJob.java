package com.knowledge.points.season.job;

import com.knowledge.points.ranking.service.QuarterTableRouter;
import com.knowledge.points.season.service.SeasonMaintenanceService;
import com.xxl.job.core.handler.annotation.XxlJob;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;

@Component
public class SeasonRankingJob {
    private final SeasonMaintenanceService maintenanceService;
    private final QuarterTableRouter tableRouter;
    private final Clock clock;

    public SeasonRankingJob(SeasonMaintenanceService maintenanceService,
                            QuarterTableRouter tableRouter, Clock clock) {
        this.maintenanceService = maintenanceService;
        this.tableRouter = tableRouter;
        this.clock = clock;
    }

    @XxlJob("seasonRankingSnapshot")
    public void snapshot() {
        maintenanceService.snapshot(tableRouter.route(Instant.now(clock)).season(), 1000);
    }

    @XxlJob("archivePointLedger")
    public void archivePreviousSeason() {
        maintenanceService.archive(tableRouter.route(Instant.now(clock).minus(100, ChronoUnit.DAYS)));
    }
}
