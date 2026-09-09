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

@Service
public class PointGrantTransactionService {

    private final PointLedgerMapper ledgerMapper;
    private final PointAccountMapper accountMapper;
    private final QuarterTableRouter tableRouter;
    private final Clock clock;
    private final SeasonAccountMapper seasonAccountMapper;

    public PointGrantTransactionService(PointLedgerMapper ledgerMapper, PointAccountMapper accountMapper,
                                        QuarterTableRouter tableRouter, Clock clock,
                                        SeasonAccountMapper seasonAccountMapper) {
        this.ledgerMapper = ledgerMapper;
        this.accountMapper = accountMapper;
        this.tableRouter = tableRouter;
        this.clock = clock;
        this.seasonAccountMapper = seasonAccountMapper;
    }

    @Transactional
    public PointGrantResultBO grant(PointGrantEventDTO event) {
        QuarterTableRouteBO route = tableRouter.route(event.occurredAt());
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
