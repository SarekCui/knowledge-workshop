package com.knowledge.points.season.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.knowledge.common.exception.BusinessException;
import com.knowledge.points.ranking.service.QuarterTableRouter;
import com.knowledge.points.ranking.service.SeasonRankingQueryService;
import com.knowledge.points.season.bo.SeasonBO;
import com.knowledge.points.season.converter.SeasonConverter;
import com.knowledge.points.season.dao.mapper.SeasonArchiveMapper;
import com.knowledge.points.season.dao.mapper.SeasonMapper;
import com.knowledge.points.season.dao.mapper.SeasonSnapshotMapper;
import com.knowledge.points.season.dao.model.SeasonArchiveDO;
import com.knowledge.points.season.dao.model.SeasonSnapshotDO;
import com.knowledge.points.season.dao.model.SeasonDO;
import com.knowledge.points.season.enums.SeasonStatus;
import com.knowledge.points.season.rule.SeasonSettlementRule;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SeasonSettlementTransactionService {
    @Resource
    private SeasonMapper seasonMapper;
    @Resource
    private SeasonSnapshotMapper snapshotMapper;
    @Resource
    private SeasonArchiveMapper archiveMapper;
    @Resource
    private SeasonRankingQueryService rankingQueryService;
    @Resource
    private QuarterTableRouter tableRouter;
    @Resource
    private Clock clock;

    @Transactional
    public SeasonBO settle(String season) {
        var item = seasonMapper.lock(season);
        if (item == null) {
            throw BusinessException.notFound("赛季不存在");
        }
        if (item.getStatus() == SeasonStatus.SETTLED) {
            return SeasonConverter.toBO(item);
        }
        Instant startsAt = item.getStartsAt().toInstant(ZoneOffset.UTC);
        Instant endsAt = item.getEndsAt().toInstant(ZoneOffset.UTC);
        SeasonSettlementRule.validate(endsAt, Instant.now(clock), 0);
        SeasonSettlementRule.validate(endsAt, Instant.now(clock), rankingQueryService.outstandingTasks(startsAt, endsAt));
        var ranking = rankingQueryService.topForSettlement(season);
        LocalDateTime now = LocalDateTime.now(clock);
        // 旧作业生成的非最终快照不能作为结算结果，结算事务内重新生成。
        snapshotMapper.delete(Wrappers.<SeasonSnapshotDO>lambdaQuery().eq(SeasonSnapshotDO::getSeason, season));
        for (var score : ranking) {
            SeasonSnapshotDO snapshot = new SeasonSnapshotDO();
            snapshot.setId(UUID.randomUUID().toString());
            snapshot.setSeason(season);
            snapshot.setUserId(score.userId());
            snapshot.setScore(score.score());
            snapshot.setRankNo(score.rank());
            snapshot.setSnapshotAt(now);
            snapshotMapper.insert(snapshot);
        }
        archiveMapper.delete(Wrappers.<SeasonArchiveDO>lambdaQuery().eq(SeasonArchiveDO::getSeason, season));
        SeasonArchiveDO archive = new SeasonArchiveDO();
        archive.setId(UUID.randomUUID().toString());
        archive.setSeason(season);
        archive.setTableName(tableRouter.route(startsAt).tableName());
        archive.setStatus("READ_ONLY");
        archive.setArchivedAt(now);
        archiveMapper.insert(archive);
        item.setStatus(SeasonStatus.SETTLED);
        item.setSettledAt(now);
        item.setSnapshotCount(ranking.size());
        // updateById 默认忽略 null，使用显式条件更新清除上次失败原因。
        int updated = seasonMapper.update(null, Wrappers.<SeasonDO>lambdaUpdate()
                .eq(SeasonDO::getSeason, season)
                .set(SeasonDO::getStatus, SeasonStatus.SETTLED)
                .set(SeasonDO::getSettledAt, now)
                .set(SeasonDO::getSnapshotCount, ranking.size())
                .set(SeasonDO::getLastError, null));
        if (updated != 1) {
            throw new IllegalStateException("赛季结算状态更新失败");
        }
        // 从数据库回读，保持首次响应与重试响应的时间精度完全一致。
        return SeasonConverter.toBO(seasonMapper.selectById(season));
    }
}
