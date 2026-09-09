package com.knowledge.iam.identity.controller;

import com.knowledge.api.common.Result;
import com.knowledge.common.exception.RequestIdFilter;
import com.knowledge.iam.identity.converter.TokenPairConverter;
import com.knowledge.iam.identity.dto.LoginDTO;
import com.knowledge.iam.identity.dto.RefreshTokenDTO;
import com.knowledge.iam.identity.service.AuthenticationService;
import com.knowledge.iam.identity.vo.TokenPairVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/iam/auth")
@Tag(name = "身份认证", description = "用户登录、令牌刷新与注销接口")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    public AuthenticationController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/login")
    @Operation(summary = "账号密码登录", description = "校验 BCrypt 密码并签发短期 Bearer JWT")
    public Result<TokenPairVO> login(
            @Valid @RequestBody LoginDTO login,
            @Parameter(hidden = true) HttpServletRequest request) {
        TokenPairVO token = TokenPairConverter.toVO(
                authenticationService.login(login.username(), login.password())
        );
        return Result.ok(token, String.valueOf(request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE)));
    }

    @PostMapping("/refresh")
    @Operation(summary = "刷新访问令牌", description = "轮换刷新令牌并签发新的 Bearer JWT")
    public Result<TokenPairVO> refresh(
            @Valid @RequestBody RefreshTokenDTO refreshToken,
            @Parameter(hidden = true) HttpServletRequest request) {
        TokenPairVO token = TokenPairConverter.toVO(
                authenticationService.refresh(refreshToken.refreshToken()));
        return Result.ok(token, requestId(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "注销当前刷新会话", description = "撤销刷新令牌所属的整个令牌族")
    public Result<Void> logout(
            @Valid @RequestBody RefreshTokenDTO refreshToken,
            @Parameter(hidden = true) HttpServletRequest request) {
        authenticationService.logout(refreshToken.refreshToken());
        return Result.ok(null, requestId(request));
    }

    private String requestId(HttpServletRequest request) {
        return String.valueOf(request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE));
    }
}
