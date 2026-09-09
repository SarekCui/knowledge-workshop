package com.knowledge.points.ranking.dao.mapper;

import com.knowledge.points.ranking.bo.UserPointTotalBO;
import com.knowledge.points.ranking.dao.model.PointLedgerDO;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface PointLedgerMapper {

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
