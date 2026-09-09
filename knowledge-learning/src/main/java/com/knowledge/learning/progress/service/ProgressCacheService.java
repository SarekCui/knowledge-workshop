package com.knowledge.learning.progress.service;

import com.knowledge.learning.progress.bo.VideoProgressBO;
import com.knowledge.learning.progress.enums.ProgressStatus;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
public class ProgressCacheService {

    private static final Duration PROGRESS_TTL = Duration.ofDays(7);
    private static final DefaultRedisScript<Long> UPDATE_SCRIPT = new DefaultRedisScript<>("""
            local oldEpoch = tonumber(redis.call('HGET', KEYS[1], 'sessionEpoch') or '-1')
            local oldSequence = tonumber(redis.call('HGET', KEYS[1], 'sequence') or '-1')
            local newEpoch = tonumber(ARGV[9])
            local newSequence = tonumber(ARGV[10])
            if newEpoch > oldEpoch or (newEpoch == oldEpoch and newSequence >= oldSequence) then
              redis.call('HSET', KEYS[1],
                'courseId', ARGV[1], 'chapterId', ARGV[2], 'videoId', ARGV[3],
                'videoVersion', ARGV[4], 'resumePositionMs', ARGV[5],
                'maxPositionMs', ARGV[6], 'durationMs', ARGV[7], 'watchedSeconds', ARGV[8],
                'sessionEpoch', ARGV[9], 'sequence', ARGV[10], 'completionRate', ARGV[11],
                'status', ARGV[12], 'updatedAt', ARGV[13])
              redis.call('EXPIRE', KEYS[1], ARGV[14])
              return 1
            end
            return 0
            """, Long.class);

    private final StringRedisTemplate redisTemplate;

    public ProgressCacheService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean put(String userId, VideoProgressBO progress) {
        Long updated = redisTemplate.execute(UPDATE_SCRIPT, List.of(progressKey(userId, progress.videoId(),
                        progress.videoVersion())), progress.courseId(), progress.chapterId(), progress.videoId(),
                String.valueOf(progress.videoVersion()), String.valueOf(progress.resumePositionMs()),
                String.valueOf(progress.maxPositionMs()), String.valueOf(progress.durationMs()),
                String.valueOf(progress.watchedSeconds()), String.valueOf(progress.sessionEpoch()),
                String.valueOf(progress.sequence()), String.valueOf(progress.completionRate()),
                progress.status().name(), String.valueOf(progress.updatedAt()),
                String.valueOf(PROGRESS_TTL.toSeconds()));
        if (Long.valueOf(1).equals(updated)) {
            redisTemplate.opsForZSet().add(recentKey(userId),
                    progress.videoId() + ":" + progress.videoVersion(), System.currentTimeMillis());
            redisTemplate.expire(recentKey(userId), Duration.ofDays(30));
            return true;
        }
        return false;
    }

    public VideoProgressBO get(String userId, String videoId, int videoVersion) {
        Map<Object, Object> values = redisTemplate.opsForHash().entries(progressKey(userId, videoId, videoVersion));
        if (values.isEmpty()) {
            return null;
        }
        redisTemplate.expire(progressKey(userId, videoId, videoVersion), PROGRESS_TTL);
        return new VideoProgressBO(text(values, "courseId"), text(values, "chapterId"), text(values, "videoId"),
                integer(values, "videoVersion"), number(values, "resumePositionMs"),
                number(values, "maxPositionMs"), number(values, "durationMs"),
                number(values, "watchedSeconds"), integer(values, "completionRate"),
                ProgressStatus.valueOf(text(values, "status")), number(values, "sessionEpoch"),
                number(values, "sequence"), LocalDateTime.parse(text(values, "updatedAt")));
    }

    public Set<String> recentMembers(String userId, int limit) {
        return redisTemplate.opsForZSet().reverseRange(recentKey(userId), 0, Math.max(0, limit - 1));
    }

    public void clearProgress(String userId, String videoId, int videoVersion) {
        redisTemplate.delete(progressKey(userId, videoId, videoVersion));
    }

    private String progressKey(String userId, String videoId, int version) {
        return "kw:learning:progress:" + userId + ":" + videoId + ":" + version;
    }

    private String recentKey(String userId) {
        return "kw:learning:recent:" + userId;
    }

    private String text(Map<Object, Object> values, String key) {
        Object value = values.get(key);
        return value == null ? null : value.toString();
    }

    private long number(Map<Object, Object> values, String key) {
        return Long.parseLong(text(values, key));
    }

    private int integer(Map<Object, Object> values, String key) {
        return Integer.parseInt(text(values, key));
    }
}
