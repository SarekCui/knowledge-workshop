package com.knowledge.learning.course.rule;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class CourseCompletionRule {
    private CourseCompletionRule() {
    }

    public static int percentage(long completed, long total) {
        if (completed < 0 || total < 0 || completed > total) {
            throw new IllegalArgumentException("课程完成数必须处于0与总视频数之间");
        }
        return total == 0 ? 0 : BigDecimal.valueOf(completed).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 0, RoundingMode.DOWN).intValueExact();
    }
}
