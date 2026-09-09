package com.knowledge.points.ranking.controller;

import com.knowledge.api.common.Result;
import com.knowledge.common.exception.RequestIdFilter;
import com.knowledge.points.ranking.converter.LeaderboardConverter;
import com.knowledge.points.ranking.service.LeaderboardService;
import com.knowledge.points.ranking.vo.LeaderboardItemVO;
import jakarta.servlet.http.HttpServletRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/points/leaderboard")
@Tag(name = "赛季排行榜", description = "查询 Redis 实时赛季积分榜")
public class LeaderboardController {
    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @GetMapping
    @Operation(summary = "查询赛季排行榜", description = "按积分倒序返回排行榜，limit 会被限制在 1 到 1000")
    public Result<List<LeaderboardItemVO>> top(
                                                   @Parameter(description = "赛季，格式为 YYYY-Q[1-4]", example = "2026-Q3", required = true)
                                                   @Pattern(regexp = "\\d{4}-Q[1-4]")
                                                   @RequestParam String season,
                                                   @Parameter(description = "返回条数，范围 1—1000", example = "100")
                                                   @Min(1) @Max(1000)
                                                   @RequestParam(defaultValue = "100") int limit,
                                                   @Parameter(hidden = true) HttpServletRequest request) {
        return Result.ok(LeaderboardConverter.toVOList(leaderboardService.top(season, limit)),
                String.valueOf(request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE)));
    }
}
