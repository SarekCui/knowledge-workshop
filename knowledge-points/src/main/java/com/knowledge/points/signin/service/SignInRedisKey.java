package com.knowledge.points.signin.service;

import java.time.YearMonth;

final class SignInRedisKey {

    static final String LOCK_SPEL = "'kw:points:lock:signin:' + #userId + ':' + #date";
    private static final String BITMAP_PREFIX = "kw:points:signin:bitmap:";

    private SignInRedisKey() {
    }

    static String bitmap(String userId, YearMonth month) {
        return BITMAP_PREFIX + userId + ':' + month;
    }
}
