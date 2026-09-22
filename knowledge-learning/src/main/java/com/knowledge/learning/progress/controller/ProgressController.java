package com.knowledge.learning.progress.controller;

import com.knowledge.api.common.Result;
import com.knowledge.common.exception.RequestIdFilter;
import com.knowledge.learning.progress.converter.ProgressConverter;
import com.knowledge.learning.progress.dto.ReportProgressDTO;
import com.knowledge.learning.progress.service.PlaybackSessionService;
import com.knowledge.learning.progress.service.ProgressQueryService;
import com.knowledge.learning.progress.service.ProgressService;
import com.knowledge.learning.progress.vo.PlaybackSessionVO;
import com.knowledge.learning.progress.vo.ProgressReportVO;
import com.knowledge.learning.progress.vo.VideoProgressVO;
import com.knowledge.security.context.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/learning")
public class ProgressController {

    @Resource private PlaybackSessionService sessionService;
    @Resource private ProgressService progressService;
    @Resource private ProgressQueryService queryService;


    @PostMapping("/videos/{videoId}/sessions")
    public Result<PlaybackSessionVO> startSession(@PathVariable String videoId,
                                                  HttpServletRequest servletRequest) {
        return Result.ok(ProgressConverter.toVO(sessionService.start(UserContext.getUserId(), videoId)),
                requestId(servletRequest));
    }

    @PostMapping("/videos/{videoId}/progress")
    public Result<ProgressReportVO> report(@PathVariable String videoId,
                                           @Valid @RequestBody ReportProgressDTO request,
                                           HttpServletRequest servletRequest) {
        return Result.ok(ProgressConverter.toVO(progressService.report(UserContext.getUserId(), videoId, request)),
                requestId(servletRequest));
    }

    @GetMapping("/videos/{videoId}/progress")
    public Result<VideoProgressVO> get(@PathVariable String videoId,
                                       HttpServletRequest servletRequest) {
        return Result.ok(
                ProgressConverter.toVO(queryService.get(UserContext.getUserId(), videoId)),
                requestId(servletRequest)
        );
    }

    @GetMapping("/progress/recent")
    public Result<List<VideoProgressVO>> recent(
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit,
            HttpServletRequest servletRequest) {
        return Result.ok(queryService.recent(UserContext.getUserId(), limit).stream()
                .map(ProgressConverter::toVO).toList(), requestId(servletRequest));
    }

    private String requestId(HttpServletRequest request) {
        return String.valueOf(request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE));
    }
}
