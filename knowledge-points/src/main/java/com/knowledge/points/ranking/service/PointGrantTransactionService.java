package com.knowledge.points.ranking.service;

import com.knowledge.api.points.dto.PointGrantEventDTO;
import com.knowledge.points.ranking.bo.PointGrantResultBO;
import com.knowledge.points.ranking.bo.QuarterTableRouteBO;
import com.knowledge.points.ranking.dao.model.PointLedgerDO;
import com.knowledge.points.ranking.dao.model.SeasonAccountDO;
import com.knowledge.points.ranking.dao.mapper.PointAccountMapper;
import com.knowledge.points.ranking.dao.mapper.PointLedgerMapper;
import com.knowledge.points.ranking.dao.mapper.SeasonAccountMapper;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.annotation.Resource;
import com.knowledge.points.season.service.SeasonWriteService;
import com.knowledge.points.season.service.SeasonClosedException;

@Service
public class PointGrantTransactionService {

    @Resource
    private PointLedgerMapper ledgerMapper;
    @Resource
    private PointAccountMapper accountMapper;
    @Resource
    private QuarterTableRouter tableRouter;
    @Resource
    private Clock clock;
    @Resource
    private SeasonAccountMapper seasonAccountMapper;
    @Resource
    private SeasonWriteService seasonWriteService;

    @Transactional
    public PointGrantResultBO grant(PointGrantEventDTO event) {
        QuarterTableRouteBO route = tableRouter.route(event.occurredAt());
        if (seasonWriteService.lockAndIsSettled(route.season())) {
            if (ledgerMapper.countEvent(route.tableName(), event.eventId()) > 0) {
                return findExistingResult(event);
            }
            throw new SeasonClosedException(route.season());
        }
        LocalDateTime now = LocalDateTime.now(clock);
        PointLedgerDO ledger = new PointLedgerDO();
        ledger.setId(UUID.randomUUID().toString());
        ledger.setEventId(event.eventId());
        ledger.setUserId(event.userId());
        ledger.setPoints(event.points());
        ledger.setSourceType(event.sourceType());
        ledger.setSourceId(event.sourceId());
        ledger.setSeason(route.season());
        ledger.setOccurredAt(LocalDateTime.ofInstant(event.occurredAt(), ZoneOffset.UTC));
        ledger.setCreatedAt(now);
        int inserted = ledgerMapper.insertIfAbsent(route.tableName(), ledger);
        if (inserted == 0) {
            return findExistingResult(event);
        }
        accountMapper.addPoints(event.userId(), event.points(), now);
        seasonAccountMapper.addPoints(route.season(), event.userId(), event.points(), now);
        long seasonPoints = seasonAccountMapper.find(route.season(), event.userId()).getPoints();
        return new PointGrantResultBO(route.season(), seasonPoints);
    }

    public PointGrantResultBO findExistingResult(PointGrantEventDTO event) {
        QuarterTableRouteBO route = tableRouter.route(event.occurredAt());
        SeasonAccountDO seasonAccount = seasonAccountMapper.find(route.season(), event.userId());
        return seasonAccount == null ? null : new PointGrantResultBO(route.season(), seasonAccount.getPoints());
    }
}
