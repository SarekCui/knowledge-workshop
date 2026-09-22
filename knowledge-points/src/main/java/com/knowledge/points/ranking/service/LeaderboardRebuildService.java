package com.knowledge.points.ranking.service;

import com.knowledge.lock.annotation.DistributedLock;
import com.knowledge.points.ranking.bo.QuarterTableRouteBO;
import com.knowledge.points.ranking.dao.mapper.PointLedgerMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class LeaderboardRebuildService {

    @Resource private PointLedgerMapper ledgerMapper;
    @Resource private LeaderboardService leaderboardService;


    @DistributedLock(keys = RankingRedisKey.REBUILD_LOCK_SPEL, leaseTime = 60)
    public int rebuild(QuarterTableRouteBO route) {
        var totals = ledgerMapper.sumPointsByUser(route.tableName());
        leaderboardService.replace(route.season(), totals);
        return totals.size();
    }
}
