package com.knowledge.points.ranking.service;

import com.knowledge.points.ranking.bo.LeaderboardItemBO;
import com.knowledge.points.ranking.bo.UserPointTotalBO;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.stereotype.Service;

@Service
public class LeaderboardService {

    private static final Duration RANKING_RETENTION = Duration.ofDays(400);
    @Resource private StringRedisTemplate redisTemplate;


    public void synchronizeScore(String season, String userId, long totalPoints) {
        redisTemplate.opsForZSet().add(key(season), userId, totalPoints);
        redisTemplate.expire(key(season), RANKING_RETENTION);
    }

    public void replace(String season, List<UserPointTotalBO> totals) {
        String destination = key(season);
        String staging = destination + ":rebuild";
        redisTemplate.delete(staging);
        if (totals.isEmpty()) {
            redisTemplate.delete(destination);
            return;
        }
        for (UserPointTotalBO total : totals) {
            redisTemplate.opsForZSet().add(staging, total.userId(), total.points());
        }
        redisTemplate.expire(staging, Duration.ofMinutes(10));
        redisTemplate.rename(staging, destination);
        redisTemplate.expire(destination, RANKING_RETENTION);
    }

    public List<LeaderboardItemBO> top(String season, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 1000));
        Set<TypedTuple<String>> tuples = redisTemplate.opsForZSet()
                .reverseRangeWithScores(key(season), 0, safeLimit - 1L);
        if (tuples == null || tuples.isEmpty()) {
            return List.of();
        }
        List<LeaderboardItemBO> result = new ArrayList<>(tuples.size());
        int rank = 1;
        for (TypedTuple<String> tuple : tuples) {
            result.add(new LeaderboardItemBO(tuple.getValue(), tuple.getScore().longValue(), rank++));
        }
        return result;
    }

    static String key(String season) {
        return RankingRedisKey.season(season);
    }
}
