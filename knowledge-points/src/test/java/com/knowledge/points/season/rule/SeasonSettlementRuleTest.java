package com.knowledge.points.season.rule;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.knowledge.common.exception.BusinessException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class SeasonSettlementRuleTest {
    private static final Instant END = Instant.parse("2026-07-01T00:00:00Z");

    @Test
    void settlementAllowedAtEndButNotBeforeEnd() {
        assertThatCode(() -> SeasonSettlementRule.validate(END, END, 0)).doesNotThrowAnyException();
        assertThatThrownBy(() -> SeasonSettlementRule.validate(END, END.minusNanos(1), 0))
                .isInstanceOf(BusinessException.class).hasMessageContaining("尚未结束");
    }

    @Test
    void outstandingTasksPreventSettlementEvenAfterEnd() {
        assertThatThrownBy(() -> SeasonSettlementRule.validate(END, END.plusSeconds(1), 1))
                .isInstanceOf(BusinessException.class).hasMessageContaining("未入账");
    }
}
