package com.knowledge.points.season.bo;

import java.time.Instant;
import com.knowledge.points.season.enums.SeasonStatus;

public record SeasonBO(String season, String name, Instant startsAt, Instant endsAt, Instant createdAt,
        SeasonStatus status, Instant settledAt, int snapshotCount, String lastError) {
}
