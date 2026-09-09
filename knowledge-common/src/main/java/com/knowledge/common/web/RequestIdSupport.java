package com.knowledge.common.web;

import java.util.UUID;
import java.util.regex.Pattern;

public final class RequestIdSupport {

    public static final String HEADER = "X-Request-Id";
    private static final Pattern SAFE_REQUEST_ID = Pattern.compile("[A-Za-z0-9._:-]{1,64}");

    private RequestIdSupport() {
    }

    public static String resolve(String candidate) {
        if (candidate == null || !SAFE_REQUEST_ID.matcher(candidate).matches()) {
            return UUID.randomUUID().toString();
        }
        return candidate;
    }
}
