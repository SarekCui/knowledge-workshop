package com.knowledge.learning.entitlement.controller;

import com.knowledge.api.common.Result;
import com.knowledge.common.exception.RequestIdFilter;
import com.knowledge.learning.entitlement.converter.EntitlementConverter;
import com.knowledge.learning.entitlement.service.EntitlementService;
import com.knowledge.learning.entitlement.vo.CourseEntitlementVO;
import com.knowledge.security.context.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/learning/entitlements")
public class EntitlementController {

    private final EntitlementService entitlementService;

    public EntitlementController(EntitlementService entitlementService) {
        this.entitlementService = entitlementService;
    }

    @GetMapping
    public Result<List<CourseEntitlementVO>> list(HttpServletRequest servletRequest) {
        return Result.ok(
                entitlementService.listActive(UserContext.getUserId()).stream()
                        .map(EntitlementConverter::toVO).toList(),
                String.valueOf(servletRequest.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE))
        );
    }
}
