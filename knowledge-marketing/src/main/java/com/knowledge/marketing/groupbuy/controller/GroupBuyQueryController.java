package com.knowledge.marketing.groupbuy.controller;

import com.knowledge.api.common.PageVO;
import com.knowledge.api.common.Result;
import com.knowledge.common.converter.PageConverter;
import com.knowledge.common.exception.RequestIdFilter;
import com.knowledge.marketing.groupbuy.converter.GroupBuyConverter;
import com.knowledge.marketing.groupbuy.enums.TradeOrderStatus;
import com.knowledge.marketing.groupbuy.service.GroupBuyQueryService;
import com.knowledge.marketing.groupbuy.vo.GroupActivityVO;
import com.knowledge.marketing.groupbuy.vo.GroupDetailVO;
import com.knowledge.marketing.groupbuy.vo.TradeOrderVO;
import com.knowledge.security.context.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/marketing")
public class GroupBuyQueryController {

    private final GroupBuyQueryService queryService;

    public GroupBuyQueryController(GroupBuyQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/activities")
    public Result<PageVO<GroupActivityVO>> activities(
            @RequestParam(defaultValue = "1") @Min(1) int pageNo,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize,
            HttpServletRequest servletRequest) {
        return Result.ok(PageConverter.toVO(queryService.listAvailableActivities(pageNo, pageSize),
                GroupBuyConverter::toVO), requestId(servletRequest));
    }

    @GetMapping("/activities/{activityId}")
    public Result<GroupActivityVO> activity(@PathVariable String activityId,
                                             HttpServletRequest servletRequest) {
        return Result.ok(GroupBuyConverter.toVO(queryService.getAvailableActivity(activityId)),
                requestId(servletRequest));
    }

    @GetMapping("/activities/{activityId}/groups")
    public Result<PageVO<GroupDetailVO>> groups(
            @PathVariable String activityId,
            @RequestParam(defaultValue = "1") @Min(1) int pageNo,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize,
            HttpServletRequest servletRequest) {
        return Result.ok(PageConverter.toVO(queryService.listAvailableGroups(activityId, pageNo, pageSize),
                GroupBuyConverter::toVO), requestId(servletRequest));
    }

    @GetMapping("/groups/{groupId}")
    public Result<GroupDetailVO> group(@PathVariable String groupId, HttpServletRequest servletRequest) {
        return Result.ok(GroupBuyConverter.toVO(queryService.getGroup(groupId)), requestId(servletRequest));
    }

    @GetMapping("/orders")
    public Result<PageVO<TradeOrderVO>> orders(
            @RequestParam(required = false) TradeOrderStatus status,
            @RequestParam(defaultValue = "1") @Min(1) int pageNo,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize,
            HttpServletRequest servletRequest) {
        return Result.ok(PageConverter.toVO(
                queryService.listUserOrders(UserContext.getUserId(), status, pageNo, pageSize),
                GroupBuyConverter::toVO), requestId(servletRequest));
    }

    @GetMapping("/orders/{orderId}")
    public Result<TradeOrderVO> order(@PathVariable String orderId, HttpServletRequest servletRequest) {
        return Result.ok(GroupBuyConverter.toVO(queryService.getUserOrder(UserContext.getUserId(), orderId)),
                requestId(servletRequest));
    }

    private String requestId(HttpServletRequest request) {
        return String.valueOf(request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE));
    }
}
