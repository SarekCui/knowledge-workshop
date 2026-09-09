package com.knowledge.marketing.groupbuy.dto;

import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

public record PaymentDTO(
        @Schema(description = "支付渠道流水号，作为支付幂等键", example = "pay-20260903-001")
        @NotBlank String paymentTradeNo) {
}
