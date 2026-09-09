package com.knowledge.points.ranking.dao.model;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PointLedgerDO {
    private String id;
    private String eventId;
    private String userId;
    private Integer points;
    private String sourceType;
    private String sourceId;
    private String season;
    private LocalDateTime occurredAt;
    private LocalDateTime createdAt;
}
