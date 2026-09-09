package com.knowledge.marketing.groupbuy.dto;

import com.knowledge.marketing.groupbuy.enums.ActivityStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ChangeGroupActivityStatusDTO(
        @NotNull ActivityStatus status,
        @NotNull @PositiveOrZero Integer version) {
}
