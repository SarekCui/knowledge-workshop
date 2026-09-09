package com.knowledge.api.common;

import java.time.Instant;

public record Result<T>(int code, String message, String requestId, Instant timestamp, T data) {

    public static <T> Result<T> ok(T data, String requestId) {
        return new Result<>(
                ErrorCode.SUCCESS.getCode(),
                ErrorCode.SUCCESS.getMessage(),
                requestId,
                Instant.now(),
                data
        );
    }

    public static Result<Void> fail(ErrorCode errorCode, String message, String requestId) {
        return new Result<>(
                errorCode.getCode(),
                message,
                requestId,
                Instant.now(),
                null
        );
    }
}
