package com.knowledge.gateway.controller;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.CIRCUITBREAKER_EXECUTION_EXCEPTION_ATTR;

import com.knowledge.api.common.ErrorCode;
import com.knowledge.api.common.Result;
import com.knowledge.gateway.filter.RequestIdWebFilter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

@RestController
@RequestMapping("/internal/gateway/fallback")
public class GatewayFallbackController {

    @RequestMapping("/{service}")
    public ResponseEntity<Result<Void>> fallback(
            @PathVariable String service, ServerWebExchange exchange) {
        String requestId = exchange.getRequest().getHeaders().getFirst(RequestIdWebFilter.HEADER);
        String resolvedRequestId = requestId == null ? "unknown" : requestId;
        if (exchange.getAttribute(CIRCUITBREAKER_EXECUTION_EXCEPTION_ATTR) == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Result.fail(ErrorCode.NOT_FOUND, "资源不存在", resolvedRequestId));
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Result.fail(
                        ErrorCode.SERVICE_UNAVAILABLE,
                        service + " 服务暂时不可用",
                        resolvedRequestId));
    }
}
