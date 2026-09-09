package com.knowledge.points.season.service;

final class SeasonRedisKey {

    static final String SNAPSHOT_LOCK_SPEL = "'kw:points:lock:season-snapshot:' + #season";
    static final String ARCHIVE_LOCK_SPEL = "'kw:points:lock:season-archive:' + #route.season";

    private SeasonRedisKey() {
    }
}
