package com.knowledge.learning.progress.service;

final class ProgressRedisKey {

    private static final String PREFIX = "kw:learning:progress:";

    private ProgressRedisKey() {
    }

    static String snapshot(String userId, String videoId, int videoVersion) {
        return PREFIX + "snapshot:" + userId + ':' + videoId + ':' + videoVersion;
    }

    static String session(String userId, String videoId) {
        return PREFIX + "session:v2:{" + userId + ':' + videoId + '}';
    }

    static String sessionEpoch(String userId, String videoId) {
        return PREFIX + "session-epoch:v2:{" + userId + ':' + videoId + '}';
    }

    static String recent(String userId) {
        return PREFIX + "recent:" + userId;
    }
}
