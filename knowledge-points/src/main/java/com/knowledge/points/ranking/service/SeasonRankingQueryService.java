package com.knowledge.points.ranking.service;

import com.knowledge.points.ranking.bo.LeaderboardItemBO;
import com.knowledge.points.ranking.dao.mapper.PointLedgerMapper;
import com.knowledge.points.ranking.dao.mapper.SeasonAccountMapper;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class SeasonRankingQueryService {
    @Resource
    private PointLedgerMapper ledgerMapper;
    @Resource
    private SeasonAccountMapper accountMapper;
    @Resource
    private QuarterTableRouter tableRouter;

    public long outstandingTasks(Instant startsAt, Instant endsAt) {
        return ledgerMapper.countOutstandingTasks(tableRouter.route(startsAt).tableName(),
                LocalDateTime.ofInstant(startsAt, ZoneOffset.UTC).toString(),
                LocalDateTime.ofInstant(endsAt, ZoneOffset.UTC).toString());
    }

    public List<LeaderboardItemBO> topForSettlement(String season) {
        List<LeaderboardItemBO> result = new ArrayList<>();
        for (var item : accountMapper.topForSettlement(season)) {
            result.add(new LeaderboardItemBO(item.userId(), item.points(), result.size() + 1));
        }
        return result;
    }
}
