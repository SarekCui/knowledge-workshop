package com.knowledge.marketing.groupbuy.controller;

import com.knowledge.api.common.PageVO;
import com.knowledge.api.common.Result;
import com.knowledge.common.converter.PageConverter;
import com.knowledge.common.exception.RequestIdFilter;
import com.knowledge.marketing.groupbuy.converter.GroupBuyConverter;
import com.knowledge.marketing.groupbuy.dto.ChangeGroupActivityStatusDTO;
import com.knowledge.marketing.groupbuy.dto.CreateGroupActivityDTO;
import com.knowledge.marketing.groupbuy.dto.CreateGroupDTO;
import com.knowledge.marketing.groupbuy.dto.UpdateGroupActivityDTO;
import com.knowledge.marketing.groupbuy.enums.ActivityStatus;
import com.knowledge.marketing.groupbuy.service.GroupBuyManagementService;
import com.knowledge.marketing.groupbuy.service.GroupBuyQueryService;
import com.knowledge.marketing.groupbuy.vo.GroupActivityVO;
import com.knowledge.marketing.groupbuy.vo.GroupDetailVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/marketing/admin")
public class GroupBuyAdminController {

    @Resource private GroupBuyManagementService managementService;
    @Resource private GroupBuyQueryService queryService;


    @GetMapping("/activities")
    public Result<PageVO<GroupActivityVO>> activities(
            @RequestParam(required = false) ActivityStatus status,
            @RequestParam(defaultValue = "1") @Min(1) int pageNo,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize,
            HttpServletRequest servletRequest) {
        return Result.ok(PageConverter.toVO(queryService.listActivitiesForManagement(status, pageNo, pageSize),
                GroupBuyConverter::toVO), requestId(servletRequest));
    }

    @PostMapping("/activities")
    public Result<GroupActivityVO> createActivity(@Valid @RequestBody CreateGroupActivityDTO request,
                                                   HttpServletRequest servletRequest) {
        return Result.ok(GroupBuyConverter.toVO(managementService.createActivity(request)),
                requestId(servletRequest));
    }

    @PutMapping("/activities/{activityId}")
    public Result<GroupActivityVO> updateActivity(@PathVariable String activityId,
                                                   @Valid @RequestBody UpdateGroupActivityDTO request,
                                                   HttpServletRequest servletRequest) {
        return Result.ok(GroupBuyConverter.toVO(managementService.updateActivity(activityId, request)),
                requestId(servletRequest));
    }

    @PatchMapping("/activities/{activityId}/status")
    public Result<GroupActivityVO> changeStatus(@PathVariable String activityId,
                                                 @Valid @RequestBody ChangeGroupActivityStatusDTO request,
                                                 HttpServletRequest servletRequest) {
        return Result.ok(GroupBuyConverter.toVO(managementService.changeStatus(activityId, request)),
                requestId(servletRequest));
    }

    @PostMapping("/activities/{activityId}/groups")
    public Result<GroupDetailVO> createGroup(@PathVariable String activityId,
                                              @Valid @RequestBody CreateGroupDTO request,
                                              HttpServletRequest servletRequest) {
        return Result.ok(GroupBuyConverter.toVO(managementService.createGroup(activityId, request)),
                requestId(servletRequest));
    }

    private String requestId(HttpServletRequest request) {
        return String.valueOf(request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE));
    }
}
