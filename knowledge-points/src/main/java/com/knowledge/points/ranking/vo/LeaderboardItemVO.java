package com.knowledge.points.ranking.vo;

import io.swagger.v3.oas.annotations.media.Schema;

public record LeaderboardItemVO(
        @Schema(description = "用户 ID", example = "user-001") String userId,
        @Schema(description = "当前赛季积分", example = "1200") long score,
        @Schema(description = "排名，从 1 开始", example = "1") int rank) {
}
