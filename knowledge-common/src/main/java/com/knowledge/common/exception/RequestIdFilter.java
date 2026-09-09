package com.knowledge.common.exception;

import com.knowledge.common.web.RequestIdSupport;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

public class RequestIdFilter extends OncePerRequestFilter {

    public static final String HEADER = RequestIdSupport.HEADER;
    public static final String REQUEST_ID_ATTRIBUTE = RequestIdFilter.class.getName() + ".requestId";
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestIdFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = RequestIdSupport.resolve(request.getHeader(HEADER));
        request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);
        response.setHeader(HEADER, requestId);
        String previousRequestId = MDC.get("requestId");
        MDC.put("requestId", requestId);
        long startNanos = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            LOGGER.atInfo()
                    .addKeyValue("event", "http_request")
                    .addKeyValue("method", request.getMethod())
                    .addKeyValue("path", request.getRequestURI())
                    .addKeyValue("status", response.getStatus())
                    .addKeyValue("durationMs", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos))
                    .log("HTTP request completed");
            if (previousRequestId == null) {
                MDC.remove("requestId");
            } else {
                MDC.put("requestId", previousRequestId);
            }
        }
    }
}
