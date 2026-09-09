package com.knowledge.points.season.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.knowledge.lock.annotation.DistributedLock;
import com.knowledge.points.ranking.bo.LeaderboardItemBO;
import com.knowledge.points.ranking.bo.QuarterTableRouteBO;
import com.knowledge.points.ranking.service.LeaderboardService;
import com.knowledge.points.season.dao.model.JobExecutionDO;
import com.knowledge.points.season.dao.model.SeasonArchiveDO;
import com.knowledge.points.season.dao.model.SeasonSnapshotDO;
import com.knowledge.points.season.dao.mapper.JobExecutionMapper;
import com.knowledge.points.season.dao.mapper.SeasonArchiveMapper;
import com.knowledge.points.season.dao.mapper.SeasonSnapshotMapper;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SeasonMaintenanceService {

    private final LeaderboardService leaderboardService;
    private final SeasonSnapshotMapper snapshotMapper;
    private final SeasonArchiveMapper archiveMapper;
    private final JobExecutionMapper executionMapper;
    private final Clock clock;

    public SeasonMaintenanceService(LeaderboardService leaderboardService,
                                    SeasonSnapshotMapper snapshotMapper,
                                    SeasonArchiveMapper archiveMapper,
                                    JobExecutionMapper executionMapper,
                                    Clock clock) {
        this.leaderboardService = leaderboardService;
        this.snapshotMapper = snapshotMapper;
        this.archiveMapper = archiveMapper;
        this.executionMapper = executionMapper;
        this.clock = clock;
    }

    @Transactional
    @DistributedLock(keys = SeasonRedisKey.SNAPSHOT_LOCK_SPEL, leaseTime = 60)
    public int snapshot(String season, int topN) {
        if (executed("SEASON_SNAPSHOT", season)) {
            return 0;
        }
        List<LeaderboardItemBO> items = leaderboardService.top(season, topN);
        LocalDateTime now = LocalDateTime.now(clock);
        insertExecution("SEASON_SNAPSHOT", season, now);
        for (LeaderboardItemBO item : items) {
            SeasonSnapshotDO snapshot = new SeasonSnapshotDO();
            snapshot.setId(UUID.randomUUID().toString());
            snapshot.setSeason(season);
            snapshot.setUserId(item.userId());
            snapshot.setScore(item.score());
            snapshot.setRankNo(item.rank());
            snapshot.setSnapshotAt(now);
            snapshotMapper.insert(snapshot);
        }
        return items.size();
    }

    @Transactional
    @DistributedLock(keys = SeasonRedisKey.ARCHIVE_LOCK_SPEL, leaseTime = 60)
    public boolean archive(QuarterTableRouteBO route) {
        if (executed("SEASON_ARCHIVE", route.season())) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        SeasonArchiveDO archive = new SeasonArchiveDO();
        archive.setId(UUID.randomUUID().toString());
        archive.setSeason(route.season());
        archive.setTableName(route.tableName());
        archive.setStatus("READ_ONLY");
        archive.setArchivedAt(now);
        archiveMapper.insert(archive);
        insertExecution("SEASON_ARCHIVE", route.season(), now);
        return true;
    }

    private boolean executed(String jobName, String businessKey) {
        return executionMapper.selectCount(Wrappers.<JobExecutionDO>lambdaQuery()
                .eq(JobExecutionDO::getJobName, jobName)
                .eq(JobExecutionDO::getBusinessKey, businessKey)) > 0;
    }

    private void insertExecution(String jobName, String businessKey, LocalDateTime now) {
        JobExecutionDO execution = new JobExecutionDO();
        execution.setId(UUID.randomUUID().toString());
        execution.setJobName(jobName);
        execution.setBusinessKey(businessKey);
        execution.setStatus("SUCCEEDED");
        execution.setCreatedAt(now);
        execution.setFinishedAt(now);
        executionMapper.insert(execution);
    }
}
