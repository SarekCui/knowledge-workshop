package com.knowledge.learning.course.rule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;

class CourseCompletionRuleTest {
    @Test
    void emptyCourseIsNotCompleted() {
        assertThat(CourseCompletionRule.percentage(0, 0)).isZero();
    }

    @Test
    void percentageFloorsAndFullCourseReachesOneHundred() {
        assertThat(CourseCompletionRule.percentage(1, 3)).isEqualTo(33);
        assertThat(CourseCompletionRule.percentage(3, 3)).isEqualTo(100);
        assertThat(CourseCompletionRule.percentage(Long.MAX_VALUE - 1, Long.MAX_VALUE)).isEqualTo(99);
    }

    @Test
    void invalidCountsAreRejected() {
        assertThatThrownBy(() -> CourseCompletionRule.percentage(2, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CourseCompletionRule.percentage(-1, 1)).isInstanceOf(IllegalArgumentException.class);
    }
}
