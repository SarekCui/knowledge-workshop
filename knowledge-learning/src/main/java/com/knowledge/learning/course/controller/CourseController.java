package com.knowledge.learning.course.controller;

import com.knowledge.api.common.Result;
import com.knowledge.common.exception.RequestIdFilter;
import com.knowledge.learning.course.converter.CourseConverter;
import com.knowledge.learning.course.service.CourseQueryService;
import com.knowledge.learning.entitlement.service.EntitlementService;
import com.knowledge.learning.course.vo.ChapterVO;
import com.knowledge.learning.course.vo.CourseVO;
import com.knowledge.security.context.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/learning/courses")
public class CourseController {

    @Autowired
    private CourseQueryService queryService;
    @Autowired
    private EntitlementService entitlementService;

    @GetMapping
    public Result<List<CourseVO>> list(HttpServletRequest servletRequest) {
        return Result.ok(
                queryService.listPublished().stream().map(CourseConverter::toVO).toList(),
                requestId(servletRequest)
        );
    }

    @GetMapping("/{courseId}")
    public Result<CourseVO> detail(@PathVariable String courseId, HttpServletRequest servletRequest) {
        return Result.ok(CourseConverter.toVO(queryService.getPublished(courseId)), requestId(servletRequest));
    }

    @GetMapping("/{courseId}/chapters")
    public Result<List<ChapterVO>> chapters(@PathVariable String courseId, HttpServletRequest servletRequest) {
        entitlementService.requireActive(UserContext.getUserId(), courseId);
        return Result.ok(
                queryService.listPublishedChapters(courseId).stream().map(CourseConverter::toVO).toList(),
                requestId(servletRequest)
        );
    }

    private String requestId(HttpServletRequest request) {
        return String.valueOf(request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE));
    }
}
