package com.knowledge.points.season.controller;

import com.knowledge.api.common.PageVO;
import com.knowledge.api.common.Result;
import com.knowledge.common.converter.PageConverter;
import com.knowledge.common.exception.RequestIdFilter;
import com.knowledge.points.season.converter.SeasonConverter;
import com.knowledge.points.season.dto.SeasonCreateDTO;
import com.knowledge.points.season.service.SeasonManagementService;
import com.knowledge.points.season.service.SeasonSettlementService;
import com.knowledge.points.season.service.SeasonHistoryService;
import com.knowledge.points.season.converter.SeasonRankingConverter;
import com.knowledge.points.season.vo.SeasonRankingVO;
import com.knowledge.points.season.vo.SeasonVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/points/admin/seasons")
@Tag(name = "积分赛季管理")
public class SeasonAdminController {
    @Autowired
    private SeasonManagementService managementService;
    @Autowired
    private SeasonSettlementService settlementService;
    @Autowired
    private SeasonHistoryService historyService;

    @GetMapping("/{season}/ranking")
    @Operation(summary = "分页查询已结算历史榜单", description = "查询数据库最终 TOP1000 快照，每页最多100条；排名保持全局排名，不随分页重新计算。未结算返回冲突。")
    public Result<PageVO<SeasonRankingVO>> ranking(@PathVariable String season,
            @RequestParam(defaultValue = "1") @Min(1) int pageNo,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize, HttpServletRequest request) {
        return Result.ok(PageConverter.toVO(historyService.ranking(season, pageNo, pageSize), SeasonRankingConverter::toVO),
                requestId(request));
    }

    @PostMapping("/{season}/settlement")
    @Operation(summary = "结算赛季或重试失败结算", description = "季度结束后执行，以 season 为幂等键；数据库快照、归档标记和结算状态在同一事务提交。已结算请求回放，不重新生成榜单。仍有未入账任务时拒绝。")
    public Result<SeasonVO> settle(@PathVariable String season, HttpServletRequest request) {
        return Result.ok(SeasonConverter.toVO(settlementService.settle(season)), requestId(request));
    }

    @PostMapping
    @Operation(summary = "创建自然季度赛季", description = "以 season 为幂等键；同一赛季及名称重试回放，名称不同返回冲突。时间按 UTC 季度自动计算。创建配置不自动创建积分流水分表。")
    public Result<SeasonVO> create(@Valid @RequestBody SeasonCreateDTO dto, HttpServletRequest request) {
        return Result.ok(SeasonConverter.toVO(managementService.create(dto)), requestId(request));
    }

    @GetMapping("/{season}")
    @Operation(summary = "查询赛季详情")
    public Result<SeasonVO> get(@PathVariable String season, HttpServletRequest request) {
        return Result.ok(SeasonConverter.toVO(managementService.get(season)), requestId(request));
    }

    @GetMapping
    @Operation(summary = "分页查询赛季", description = "按季度开始时间倒序，每页最多 100 条")
    public Result<PageVO<SeasonVO>> list(@RequestParam(defaultValue = "1") @Min(1) int pageNo,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize, HttpServletRequest request) {
        return Result.ok(PageConverter.toVO(managementService.list(pageNo, pageSize), SeasonConverter::toVO),
                requestId(request));
    }

    private static String requestId(HttpServletRequest request) {
        return String.valueOf(request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE));
    }
}
