package com.knowledge.points.ranking.service;

final class RankingRedisKey {

    static final String REBUILD_LOCK_SPEL = "'kw:points:lock:ranking-rebuild:' + #route.season";
    private static final String SEASON_PREFIX = "kw:points:ranking:season:";

    private RankingRedisKey() {
    }

    static String season(String seasonId) {
        return SEASON_PREFIX + seasonId;
    }
}
