package com.knowledge.learning.course.controller;

import com.knowledge.api.common.Result;
import com.knowledge.common.exception.RequestIdFilter;
import com.knowledge.learning.course.converter.CourseConverter;
import com.knowledge.learning.course.dto.ChangeCourseStatusDTO;
import com.knowledge.learning.course.dto.CreateChapterDTO;
import com.knowledge.learning.course.dto.CreateCourseDTO;
import com.knowledge.learning.course.dto.UpdateChapterDTO;
import com.knowledge.learning.course.dto.UpdateCourseDTO;
import com.knowledge.learning.course.service.CourseManagementService;
import com.knowledge.learning.course.vo.ChapterVO;
import com.knowledge.learning.course.vo.CourseVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/learning/admin")
public class CourseAdminController {

    private final CourseManagementService managementService;

    public CourseAdminController(CourseManagementService managementService) {
        this.managementService = managementService;
    }

    @PostMapping("/courses")
    public Result<CourseVO> createCourse(@Valid @RequestBody CreateCourseDTO request,
                                         HttpServletRequest servletRequest) {
        return Result.ok(CourseConverter.toVO(managementService.createCourse(request)), requestId(servletRequest));
    }

    @PutMapping("/courses/{courseId}")
    public Result<CourseVO> updateCourse(@PathVariable String courseId,
                                         @Valid @RequestBody UpdateCourseDTO request,
                                         HttpServletRequest servletRequest) {
        return Result.ok(CourseConverter.toVO(managementService.updateCourse(courseId, request)),
                requestId(servletRequest));
    }

    @PatchMapping("/courses/{courseId}/status")
    public Result<CourseVO> changeStatus(@PathVariable String courseId,
                                         @Valid @RequestBody ChangeCourseStatusDTO request,
                                         HttpServletRequest servletRequest) {
        return Result.ok(CourseConverter.toVO(managementService.changeStatus(courseId, request)),
                requestId(servletRequest));
    }

    @PostMapping("/courses/{courseId}/chapters")
    public Result<ChapterVO> createChapter(@PathVariable String courseId,
                                           @Valid @RequestBody CreateChapterDTO request,
                                           HttpServletRequest servletRequest) {
        return Result.ok(CourseConverter.toVO(managementService.createChapter(courseId, request)),
                requestId(servletRequest));
    }

    @PutMapping("/chapters/{chapterId}")
    public Result<ChapterVO> updateChapter(@PathVariable String chapterId,
                                           @Valid @RequestBody UpdateChapterDTO request,
                                           HttpServletRequest servletRequest) {
        return Result.ok(CourseConverter.toVO(managementService.updateChapter(chapterId, request)),
                requestId(servletRequest));
    }

    @DeleteMapping("/chapters/{chapterId}")
    public Result<Void> deleteChapter(@PathVariable String chapterId,
                                      @RequestParam @PositiveOrZero int version,
                                      HttpServletRequest servletRequest) {
        managementService.deleteChapter(chapterId, version);
        return Result.ok(null, requestId(servletRequest));
    }

    private String requestId(HttpServletRequest request) {
        return String.valueOf(request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE));
    }
}
