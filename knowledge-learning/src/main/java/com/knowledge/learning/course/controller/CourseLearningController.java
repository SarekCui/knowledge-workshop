package com.knowledge.learning.course.controller;

import com.knowledge.api.common.PageVO;
import com.knowledge.api.common.Result;
import com.knowledge.common.converter.PageConverter;
import com.knowledge.common.exception.RequestIdFilter;
import com.knowledge.learning.course.converter.CourseLearningConverter;
import com.knowledge.learning.course.service.CourseLearningService;
import com.knowledge.learning.course.vo.CourseLearningVO;
import com.knowledge.security.context.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/learning")
@Tag(name = "课程学习进度")
public class CourseLearningController {
    @Resource
    private CourseLearningService learningService;

    @GetMapping("/my-courses")
    @Operation(summary = "分页查询我的可学习课程", description = "仅返回本人有效权益下的已发布课程，按课程去重；进度来自数据库当前发布视频版本，每页最多100条。")
    public Result<PageVO<CourseLearningVO>> mine(@RequestParam(defaultValue = "1") @Min(1) int pageNo,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize, HttpServletRequest request) {
        return Result.ok(PageConverter.toVO(learningService.pageMine(UserContext.getUserId(), pageNo, pageSize),
                CourseLearningConverter::toVO), requestId(request));
    }

    @GetMapping("/courses/{courseId}/progress")
    @Operation(summary = "查询本人课程学习进度", description = "完成率为已完成视频数/当前发布视频数的整数百分比，向下取整；返回最近已落库位置，无学习记录时返回首个可学习视频，异步消费未完成时可能短暂滞后。")
    public Result<CourseLearningVO> progress(@PathVariable String courseId, HttpServletRequest request) {
        return Result.ok(CourseLearningConverter.toVO(learningService.get(UserContext.getUserId(), courseId)), requestId(request));
    }

    private static String requestId(HttpServletRequest request) {
        return String.valueOf(request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE));
    }
}
