package com.knowledge.iam.identity.controller;

import com.knowledge.api.common.Result;
import com.knowledge.api.common.ErrorCode;
import com.knowledge.common.exception.BusinessException;
import com.knowledge.common.exception.RequestIdFilter;
import com.knowledge.iam.identity.bo.TokenPairBO;
import com.knowledge.iam.identity.converter.TokenPairConverter;
import com.knowledge.iam.identity.dto.LoginDTO;
import com.knowledge.iam.identity.service.AuthenticationService;
import com.knowledge.iam.identity.service.WebAuthCookieService;
import com.knowledge.iam.identity.vo.WebAccessTokenVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/iam/web/auth")
@Tag(name = "浏览器认证")
public class WebAuthenticationController {

    @Autowired
    private AuthenticationService authenticationService;
    @Autowired
    private WebAuthCookieService cookieService;

    @PostMapping("/login")
    @Operation(summary = "浏览器登录")
    public Result<WebAccessTokenVO> login(@Valid @RequestBody LoginDTO login,
                                         HttpServletRequest request, HttpServletResponse response) {
        prepare(request, response);
        String previous = cookieService.read(request);
        TokenPairBO pair = authenticationService.login(login.username(), login.password());
        if (previous != null) authenticationService.logout(previous);
        return issue(pair, request, response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "浏览器刷新并恢复登录")
    public Result<WebAccessTokenVO> refresh(HttpServletRequest request, HttpServletResponse response) {
        prepare(request, response);
        String token = cookieService.read(request);
        if (token == null) throw BusinessException.unauthorized("请登录后继续");
        try {
            return issue(authenticationService.refresh(token), request, response);
        } catch (BusinessException rejected) {
            if (rejected.getErrorCode() == ErrorCode.UNAUTHORIZED) cookieService.clear(response);
            throw rejected;
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "浏览器退出登录")
    public Result<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        prepare(request, response);
        try {
            String token = cookieService.read(request);
            if (token != null) authenticationService.logout(token);
        } finally {
            cookieService.clear(response);
        }
        return Result.ok(null, requestId(request));
    }

    private void prepare(HttpServletRequest request, HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        cookieService.requireTrustedRequest(request);
    }

    private Result<WebAccessTokenVO> issue(TokenPairBO pair, HttpServletRequest request, HttpServletResponse response) {
        cookieService.write(response, pair.refreshToken(), pair.refreshTokenExpiresInSeconds());
        return Result.ok(TokenPairConverter.toWebVO(pair), requestId(request));
    }

    private String requestId(HttpServletRequest request) {
        return String.valueOf(request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE));
    }
}
