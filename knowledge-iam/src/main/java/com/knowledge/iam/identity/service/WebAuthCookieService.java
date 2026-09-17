package com.knowledge.iam.identity.service;

import com.knowledge.common.exception.BusinessException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Service
public class WebAuthCookieService {

    static final String COOKIE_NAME = "kw_refresh";
    static final String COOKIE_PATH = "/api/iam/web/auth";

    @Value("${knowledge.security.web.allowed-origins:http://127.0.0.1:5173,http://localhost:5173}")
    private String allowedOrigins;
    @Value("${knowledge.security.web.cookie-secure:true}")
    private boolean secure;

    public void requireTrustedRequest(HttpServletRequest request) {
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        boolean trusted = origin != null && Arrays.stream(allowedOrigins.split(","))
                .map(String::trim).anyMatch(origin::equals);
        if (!trusted || !"1".equals(request.getHeader("X-Web-Auth"))) {
            throw BusinessException.forbidden("浏览器认证请求来源不合法");
        }
    }

    public String read(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        String token = null;
        for (Cookie cookie : request.getCookies()) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                if (token != null) throw BusinessException.badRequest("刷新凭证重复");
                token = cookie.getValue();
            }
        }
        return token == null || token.isBlank() ? null : token;
    }

    public void write(HttpServletResponse response, String token, long maxAgeSeconds) {
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, token).path(COOKIE_PATH)
                .httpOnly(true).secure(secure).sameSite("Strict").maxAge(maxAgeSeconds).build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void clear(HttpServletResponse response) {
        write(response, "", 0);
    }
}
