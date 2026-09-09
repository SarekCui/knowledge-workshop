package com.knowledge.api.common;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    SUCCESS(HttpStatus.OK, "success"),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "请求参数错误"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "未认证"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "无权限"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "资源不存在"),
    CONFLICT(HttpStatus.CONFLICT, "资源状态冲突"),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "请求过于频繁"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "服务器内部错误"),
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "服务暂时不可用");

    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public int getCode() {
        return httpStatus.value();
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getMessage() {
        return message;
    }
}
