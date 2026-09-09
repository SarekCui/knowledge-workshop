package com.knowledge.points.signin.vo;

import java.time.LocalDate;
import io.swagger.v3.oas.annotations.media.Schema;

public record SignInVO(
        @Schema(description = "用户 ID", example = "user-001") String userId,
        @Schema(description = "UTC 签到日期", example = "2026-09-03") LocalDate signDate,
        @Schema(description = "连续签到天数", example = "3") int continuousDays,
        @Schema(description = "本次奖励积分", example = "10") int rewardPoints,
        @Schema(description = "是否为当天重复签到", example = "false") boolean duplicated) {
}
