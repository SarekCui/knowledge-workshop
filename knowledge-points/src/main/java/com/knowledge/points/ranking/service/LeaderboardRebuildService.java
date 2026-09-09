package com.knowledge.points.ranking.service;

import com.knowledge.lock.annotation.DistributedLock;
import com.knowledge.points.ranking.bo.QuarterTableRouteBO;
import com.knowledge.points.ranking.dao.mapper.PointLedgerMapper;
import org.springframework.stereotype.Service;

@Service
public class LeaderboardRebuildService {

    private final PointLedgerMapper ledgerMapper;
    private final LeaderboardService leaderboardService;

    public LeaderboardRebuildService(PointLedgerMapper ledgerMapper, LeaderboardService leaderboardService) {
        this.ledgerMapper = ledgerMapper;
        this.leaderboardService = leaderboardService;
    }

    @DistributedLock(keys = RankingRedisKey.REBUILD_LOCK_SPEL, leaseTime = 60)
    public int rebuild(QuarterTableRouteBO route) {
        var totals = ledgerMapper.sumPointsByUser(route.tableName());
        leaderboardService.replace(route.season(), totals);
        return totals.size();
    }
}
