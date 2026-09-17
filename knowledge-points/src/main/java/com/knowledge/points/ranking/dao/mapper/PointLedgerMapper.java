package com.knowledge.points.ranking.dao.mapper;

import com.knowledge.points.ranking.bo.UserPointTotalBO;
import com.knowledge.points.ranking.dao.model.PointLedgerDO;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface PointLedgerMapper {
    @Select("SELECT COUNT(*) FROM ${tableName} WHERE event_id = #{eventId}")
    int countEvent(@Param("tableName") String validatedTableName, @Param("eventId") String eventId);

    @Select("""
            SELECT COUNT(*) FROM point_task t
            WHERE t.occurred_at >= #{startsAt} AND t.occurred_at < #{endsAt}
              AND NOT EXISTS (SELECT 1 FROM ${tableName} l WHERE l.event_id = t.event_id)
            """)
    long countOutstandingTasks(@Param("tableName") String validatedTableName,
            @Param("startsAt") String startsAt, @Param("endsAt") String endsAt);

    @Insert("""
            INSERT IGNORE INTO ${tableName}
              (id, event_id, user_id, points, source_type, source_id, season, occurred_at, created_at)
            VALUES
              (#{ledger.id}, #{ledger.eventId}, #{ledger.userId}, #{ledger.points}, #{ledger.sourceType},
               #{ledger.sourceId}, #{ledger.season}, #{ledger.occurredAt}, #{ledger.createdAt})
            """)
    int insertIfAbsent(@Param("tableName") String validatedTableName, @Param("ledger") PointLedgerDO ledger);

    @Select("""
            SELECT user_id, SUM(points) AS points
              FROM ${tableName}
             GROUP BY user_id
            """)
    List<UserPointTotalBO> sumPointsByUser(@Param("tableName") String validatedTableName);
}
