package com.knowledge.points.signin.controller;

import com.knowledge.api.common.Result;
import com.knowledge.common.exception.RequestIdFilter;
import com.knowledge.points.signin.converter.SignInConverter;
import com.knowledge.points.signin.service.SignInService;
import com.knowledge.points.signin.vo.SignInVO;
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

    private final SignInService signInService;
    private final Clock clock;

    public SignInController(SignInService signInService, Clock clock) {
        this.signInService = signInService;
        this.clock = clock;
    }

    @PostMapping
    @Operation(summary = "每日签到", description = "同一用户同一天重复签到不会重复发放积分")
    public Result<SignInVO> sign(@Parameter(hidden = true) HttpServletRequest servletRequest) {
        return Result.ok(SignInConverter.toVO(
                        signInService.sign(UserContext.getUserId(), LocalDate.now(clock))),
                String.valueOf(servletRequest.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE)));
    }
}
