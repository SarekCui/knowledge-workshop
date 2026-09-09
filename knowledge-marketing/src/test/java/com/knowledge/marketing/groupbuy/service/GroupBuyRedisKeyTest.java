package com.knowledge.marketing.groupbuy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class GroupBuyRedisKeyTest {

    @Test
    void keysUsedByOneGroupShareRedisClusterHashTag() {
        assertThat(GroupBuyRedisKey.occupied("group-1")).contains("{group-1}");
        assertThat(GroupBuyRedisKey.reservation("group-1", "user-1")).contains("{group-1}");
        assertThat(GroupBuyRedisKey.reservationExpiry("group-1")).contains("{group-1}");
    }

    @Test
    void rejectsIdentifiersThatCanBreakKeyStructure() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> GroupBuyRedisKey.reservation("group:1", "user-1"));
    }
}
