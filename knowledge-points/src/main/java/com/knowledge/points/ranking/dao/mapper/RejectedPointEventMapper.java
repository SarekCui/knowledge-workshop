package com.knowledge.points.ranking.dao.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

public interface RejectedPointEventMapper {
    @Insert("""
            INSERT INTO rejected_point_event(event_id, payload, reason, created_at)
            VALUES(#{eventId}, #{payload}, #{reason}, UTC_TIMESTAMP())
            ON DUPLICATE KEY UPDATE event_id = VALUES(event_id)
            """)
    int insertIfAbsent(@Param("eventId") String eventId, @Param("payload") String payload, @Param("reason") String reason);
}
