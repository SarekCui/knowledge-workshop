package com.knowledge.learning.course.controller;

import com.knowledge.api.common.PageVO;
import com.knowledge.api.common.Result;
import com.knowledge.common.exception.RequestIdFilter;
import com.knowledge.learning.course.converter.CourseCatalogConverter;
import com.knowledge.learning.course.service.CourseCatalogService;
import com.knowledge.learning.course.vo.CourseCatalogDetailVO;
import com.knowledge.learning.course.vo.CourseCatalogVO;
import com.knowledge.learning.course.vo.CourseCategoryVO;
import com.knowledge.security.context.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.List;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/learning/catalog")
public class CourseCatalogController {
    @Resource
    private CourseCatalogService courseCatalogService;

    @GetMapping("/categories")
    public Result<List<CourseCategoryVO>> categories(HttpServletRequest request) {
        return Result.ok(courseCatalogService.categories().stream().map(CourseCatalogConverter::toVO).toList(),
                requestId(request));
    }

    @GetMapping("/courses")
    public Result<PageVO<CourseCatalogVO>> courses(
            @RequestParam(required = false) @Size(max = 64) String categoryId,
            @RequestParam(required = false) @Size(max = 100) String keyword,
            @RequestParam(defaultValue = "1") @Min(1) int pageNo,
            @RequestParam(defaultValue = "12") @Min(1) @Max(50) int pageSize,
            HttpServletRequest request) {
        var page = courseCatalogService.page(UserContext.getUserIdOrNull(), categoryId, keyword, pageNo, pageSize);
        return Result.ok(new PageVO<>(page.items().stream().map(CourseCatalogConverter::toVO).toList(),
                page.pageNo(), page.pageSize(), page.total()), requestId(request));
    }

    @GetMapping("/courses/{courseId}")
    public Result<CourseCatalogDetailVO> detail(@PathVariable @Size(max = 64) String courseId,
            HttpServletRequest request) {
        return Result.ok(CourseCatalogConverter.toVO(
                courseCatalogService.detail(UserContext.getUserIdOrNull(), courseId)), requestId(request));
    }

    private String requestId(HttpServletRequest request) {
        return String.valueOf(request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE));
    }
}
