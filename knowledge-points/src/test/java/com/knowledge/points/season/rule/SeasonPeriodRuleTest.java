package com.knowledge.points.season.rule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.knowledge.common.exception.BusinessException;
import com.knowledge.points.season.converter.SeasonConverter;
import com.knowledge.points.season.dto.SeasonCreateDTO;
import java.time.Instant;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class SeasonPeriodRuleTest {
    @Test
    void quarterBoundariesAreContiguousAndUtc() {
        var item = SeasonConverter.toDO(new SeasonCreateDTO("2026-Q4", " 第四季度 "), Instant.EPOCH);
        assertThat(item.getStartsAt()).isEqualTo(LocalDateTime.parse("2026-10-01T00:00:00"));
        assertThat(item.getEndsAt()).isEqualTo(SeasonPeriodRule.startsAt("2027-Q1"));
        assertThat(item.getName()).isEqualTo("第四季度");
        assertThat(SeasonConverter.toBO(item).startsAt()).isEqualTo(Instant.parse("2026-10-01T00:00:00Z"));
    }

    @Test
    void invalidQuarterAndUnsupportedDatabaseYearsAreRejected() {
        for (String value : new String[]{"2026-Q0", "2026-Q5", "2026-q1", "0000-Q1", "0999-Q1", "9999-Q4", "wrong"}) {
            assertThatThrownBy(() -> SeasonPeriodRule.startsAt(value)).isInstanceOf(BusinessException.class);
        }
        assertThatThrownBy(() -> SeasonPeriodRule.startsAt(null)).isInstanceOf(BusinessException.class);
    }
}
