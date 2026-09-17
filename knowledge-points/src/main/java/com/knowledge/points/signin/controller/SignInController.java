package com.knowledge.points.signin.controller;

import com.knowledge.api.common.Result;
import com.knowledge.common.exception.RequestIdFilter;
import com.knowledge.points.signin.converter.SignInConverter;
import com.knowledge.points.signin.service.SignInService;
import com.knowledge.points.signin.vo.SignInVO;
import com.knowledge.points.signin.vo.SignInMonthVO;
import com.knowledge.points.signin.service.SignInQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.knowledge.security.context.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/points/sign-ins")
@Tag(name = "每日签到", description = "记录签到并异步创建积分任务")
public class SignInController {

    @Autowired
    private SignInService signInService;
    @Autowired
    private Clock clock;
    @Autowired
    private SignInQueryService queryService;

    @GetMapping
    @Operation(summary = "查询本人月度签到轨迹", description = "月份格式 YYYY-MM；从数据库查询本人记录，最多31个日期，按 UTC 日期展示")
    public Result<SignInMonthVO> month(@RequestParam String month, HttpServletRequest request) {
        return Result.ok(SignInConverter.toVO(queryService.month(UserContext.getUserId(), month)),
                String.valueOf(request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE)));
    }

    @PostMapping
    @Operation(summary = "每日签到", description = "同一用户同一天重复签到不会重复发放积分")
    public Result<SignInVO> sign(@Parameter(hidden = true) HttpServletRequest servletRequest) {
        return Result.ok(SignInConverter.toVO(
                        signInService.sign(UserContext.getUserId(), LocalDate.now(clock))),
                String.valueOf(servletRequest.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE)));
    }
}
