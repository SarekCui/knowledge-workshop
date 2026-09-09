package com.knowledge.marketing.groupbuy.controller;

import com.knowledge.api.common.Result;
import com.knowledge.common.exception.RequestIdFilter;
import com.knowledge.marketing.groupbuy.bo.JoinGroupBO;
import com.knowledge.marketing.groupbuy.bo.TradeOrderBO;
import com.knowledge.marketing.groupbuy.converter.GroupBuyConverter;
import com.knowledge.marketing.groupbuy.dto.PaymentDTO;
import com.knowledge.marketing.groupbuy.service.GroupPurchaseService;
import com.knowledge.marketing.groupbuy.service.PaymentSettlementService;
import com.knowledge.marketing.groupbuy.vo.TradeOrderVO;
import com.knowledge.security.context.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/marketing")
@Tag(name = "知识付费拼团", description = "参加拼团及模拟支付接口")
public class GroupPurchaseController {

    private final GroupPurchaseService purchaseService;
    private final PaymentSettlementService settlementService;

    public GroupPurchaseController(GroupPurchaseService purchaseService,
                                   PaymentSettlementService settlementService) {
        this.purchaseService = purchaseService;
        this.settlementService = settlementService;
    }

    @PostMapping("/groups/{groupId}/join")
    @Operation(summary = "参加拼团", description = "同一用户重复加入同一团时返回已有订单")
    public Result<TradeOrderVO> join(
                                     @Parameter(description = "团实例 ID", example = "group-demo", required = true)
                                     @PathVariable String groupId,
                                     @Parameter(hidden = true) HttpServletRequest servletRequest) {
        return Result.ok(GroupBuyConverter.toVO(
                purchaseService.join(new JoinGroupBO(groupId, UserContext.getUserId()))),
                requestId(servletRequest));
    }

    @PostMapping("/orders/{orderId}/pay")
    @Operation(summary = "支付拼团订单", description = "使用支付流水号完成模拟支付；重复流水安全返回既有结果")
    public Result<TradeOrderVO> pay(
            @Parameter(description = "订单 ID", example = "order-001", required = true)
            @PathVariable String orderId,
            @Valid @RequestBody PaymentDTO request,
            @Parameter(hidden = true) HttpServletRequest servletRequest) {
        TradeOrderBO paid = settlementService.settle(
                orderId, request.paymentTradeNo(), UserContext.getUserId());
        return Result.ok(GroupBuyConverter.toVO(paid), requestId(servletRequest));
    }

    private String requestId(HttpServletRequest request) {
        return String.valueOf(request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE));
    }
}
