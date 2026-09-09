package com.knowledge.marketing.notification.controller;

import com.knowledge.api.common.PageVO;
import com.knowledge.api.common.Result;
import com.knowledge.common.converter.PageConverter;
import com.knowledge.common.exception.RequestIdFilter;
import com.knowledge.marketing.notification.converter.NotificationTaskConverter;
import com.knowledge.marketing.notification.enums.NotificationStatus;
import com.knowledge.marketing.notification.service.NotificationTaskManagementService;
import com.knowledge.marketing.notification.vo.NotificationTaskVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/marketing/admin/notification-tasks")
public class NotificationTaskAdminController {

    private final NotificationTaskManagementService managementService;

    public NotificationTaskAdminController(NotificationTaskManagementService managementService) {
        this.managementService = managementService;
    }

    @GetMapping
    public Result<PageVO<NotificationTaskVO>> list(
            @RequestParam(required = false) NotificationStatus status,
            @RequestParam(defaultValue = "1") @Min(1) int pageNo,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize,
            HttpServletRequest servletRequest) {
        return Result.ok(PageConverter.toVO(managementService.list(status, pageNo, pageSize),
                NotificationTaskConverter::toVO), requestId(servletRequest));
    }

    @PostMapping("/{taskId}/retry")
    public Result<NotificationTaskVO> retry(@PathVariable String taskId, HttpServletRequest servletRequest) {
        return Result.ok(NotificationTaskConverter.toVO(managementService.retry(taskId)),
                requestId(servletRequest));
    }

    private String requestId(HttpServletRequest request) {
        return String.valueOf(request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE));
    }
}
