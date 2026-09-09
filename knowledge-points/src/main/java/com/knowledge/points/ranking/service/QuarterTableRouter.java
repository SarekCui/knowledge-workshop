package com.knowledge.points.ranking.service;

import com.knowledge.points.ranking.bo.QuarterTableRouteBO;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class QuarterTableRouter {
    private static final Pattern SAFE_TABLE = Pattern.compile("point_ledger_\\d{4}_q[1-4]");

    public QuarterTableRouteBO route(Instant instant) {
        ZonedDateTime utc = instant.atZone(ZoneOffset.UTC);
        int quarter = (utc.getMonthValue() - 1) / 3 + 1;
        String season = utc.getYear() + "-Q" + quarter;
        String table = "point_ledger_" + utc.getYear() + "_q" + quarter;
        if (!SAFE_TABLE.matcher(table).matches()) {
            throw new IllegalArgumentException("非法积分明细表名");
        }
        return new QuarterTableRouteBO(season, table);
    }
}
