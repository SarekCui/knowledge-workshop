package com.knowledge.points.season.rule;

import com.knowledge.common.exception.BusinessException;
import java.time.LocalDateTime;

public final class SeasonPeriodRule {
    private SeasonPeriodRule() {
    }

    public static LocalDateTime startsAt(String season) {
        if (season == null || !season.matches("\\d{4}-Q[1-4]") || season.startsWith("0000")) {
            throw BusinessException.badRequest("赛季格式必须为 YYYY-Q[1-4]，年份不能为 0000");
        }
        int year = Integer.parseInt(season.substring(0, 4));
        int quarter = Integer.parseInt(season.substring(6));
        if (year < 1000 || (year == 9999 && quarter == 4)) {
            throw BusinessException.badRequest("赛季结束时间超出数据库支持范围");
        }
        return LocalDateTime.of(year, (quarter - 1) * 3 + 1, 1, 0, 0);
    }
}
