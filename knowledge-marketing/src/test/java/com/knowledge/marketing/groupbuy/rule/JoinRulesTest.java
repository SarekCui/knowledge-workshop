package com.knowledge.marketing.groupbuy.rule;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.knowledge.api.common.ErrorCode;
import com.knowledge.common.exception.BusinessException;
import com.knowledge.marketing.groupbuy.dao.model.GroupActivityDO;
import com.knowledge.marketing.groupbuy.dao.model.GroupOrderDO;
import com.knowledge.marketing.groupbuy.bo.JoinGroupBO;
import com.knowledge.marketing.groupbuy.enums.ActivityStatus;
import com.knowledge.marketing.groupbuy.enums.GroupStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class JoinRulesTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-09-03T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void rejectsExpiredActivityBeforeCapacityChecks() {
        JoinValidationContext context = context();
        context.activity().setEndTime(LocalDateTime.of(2026, 9, 3, 0, 0));

        assertThatThrownBy(() -> new ActivityAvailableRule(clock).check(context))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> {
                            org.assertj.core.api.Assertions.assertThat(exception.getErrorCode())
                                    .isEqualTo(ErrorCode.CONFLICT);
                            org.assertj.core.api.Assertions.assertThat(exception.getMessage())
                                    .isEqualTo("拼团活动已结束");
                        });
    }

    @Test
    void rejectsParticipationLimitAndFullGroup() {
        JoinValidationContext limited = new JoinValidationContext(context().request(), context().activity(),
                context().group(), 2);
        assertThatThrownBy(() -> new ParticipationLimitRule().check(limited))
                .isInstanceOf(BusinessException.class);

        JoinValidationContext full = context();
        full.group().setConfirmedCount(3);
        assertThatThrownBy(() -> new GroupCapacityRule(clock).check(full))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> {
                            org.assertj.core.api.Assertions.assertThat(exception.getErrorCode())
                                    .isEqualTo(ErrorCode.CONFLICT);
                            org.assertj.core.api.Assertions.assertThat(exception.getMessage())
                                    .isEqualTo("拼团名额已满");
                        });
    }

    private JoinValidationContext context() {
        GroupActivityDO activity = new GroupActivityDO();
        activity.setStatus(ActivityStatus.ACTIVE);
        activity.setStartTime(LocalDateTime.of(2026, 9, 1, 0, 0));
        activity.setEndTime(LocalDateTime.of(2026, 9, 10, 0, 0));
        activity.setMaxJoinPerUser(2);
        GroupOrderDO group = new GroupOrderDO();
        group.setStatus(GroupStatus.FORMING);
        group.setExpiresAt(LocalDateTime.of(2026, 9, 4, 0, 0));
        group.setConfirmedCount(0);
        group.setTargetCount(3);
        return new JoinValidationContext(new JoinGroupBO("r1", "a1", "g1", "u1"), activity, group, 0);
    }
}
