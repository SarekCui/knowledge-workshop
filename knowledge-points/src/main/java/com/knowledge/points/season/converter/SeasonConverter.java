package com.knowledge.points.season.converter;

import com.knowledge.points.season.bo.SeasonBO;
import com.knowledge.points.season.dao.model.SeasonDO;
import com.knowledge.points.season.dto.SeasonCreateDTO;
import com.knowledge.points.season.rule.SeasonPeriodRule;
import com.knowledge.points.season.vo.SeasonVO;
import com.knowledge.points.season.enums.SeasonStatus;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public final class SeasonConverter {
    private SeasonConverter() {
    }

    public static SeasonDO toDO(SeasonCreateDTO dto, Instant now) {
        SeasonDO item = new SeasonDO();
        item.setSeason(dto.season());
        item.setName(dto.name().strip());
        item.setStartsAt(SeasonPeriodRule.startsAt(dto.season()));
        item.setEndsAt(item.getStartsAt().plusMonths(3));
        item.setCreatedAt(LocalDateTime.ofInstant(now, ZoneOffset.UTC));
        item.setStatus(SeasonStatus.READY);
        item.setSnapshotCount(0);
        return item;
    }

    public static SeasonBO toBO(SeasonDO item) {
        return new SeasonBO(item.getSeason(), item.getName(), item.getStartsAt().toInstant(ZoneOffset.UTC),
                item.getEndsAt().toInstant(ZoneOffset.UTC), item.getCreatedAt().toInstant(ZoneOffset.UTC),
                item.getStatus(), item.getSettledAt() == null ? null : item.getSettledAt().toInstant(ZoneOffset.UTC),
                item.getSnapshotCount(), item.getLastError());
    }

    public static SeasonVO toVO(SeasonBO item) {
        return new SeasonVO(item.season(), item.name(), item.startsAt(), item.endsAt(), item.createdAt(),
                item.status(), item.settledAt(), item.snapshotCount(), item.lastError());
    }
}
