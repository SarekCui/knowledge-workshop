package com.knowledge.points.ranking.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.knowledge.points.ranking.bo.QuarterTableRouteBO;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class QuarterTableRouterTest {

    private final QuarterTableRouter router = new QuarterTableRouter();

    @Test
    void routesQuarterBoundariesInUtc() {
        assertThat(router.route(Instant.parse("2026-03-31T23:59:59Z")))
                .isEqualTo(new QuarterTableRouteBO("2026-Q1", "point_ledger_2026_q1"));
        assertThat(router.route(Instant.parse("2026-04-01T00:00:00Z")))
                .isEqualTo(new QuarterTableRouteBO("2026-Q2", "point_ledger_2026_q2"));
        assertThat(router.route(Instant.parse("2026-12-31T23:59:59Z")))
                .isEqualTo(new QuarterTableRouteBO("2026-Q4", "point_ledger_2026_q4"));
    }
}
