package com.knowledge.marketing.groupbuy.dto;

import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

public record JoinGroupDTO(
        @Schema(description = "客户端业务幂等键", example = "join-20260903-001") @NotBlank String requestId,
        @Schema(description = "拼团活动 ID", example = "activity-demo") @NotBlank String activityId,
        @Schema(description = "团实例 ID", example = "group-demo") @NotBlank String groupId) {
}
