package com.knowledge.learning.progress.service;

import com.knowledge.common.exception.BusinessException;
import com.knowledge.learning.course.bo.ChapterBO;
import com.knowledge.learning.course.service.CourseQueryService;
import com.knowledge.learning.entitlement.service.EntitlementService;
import com.knowledge.learning.progress.bo.PlaybackSessionBO;
import com.knowledge.learning.progress.bo.VideoProgressBO;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
public class PlaybackSessionService {

    private static final DefaultRedisScript<Long> NEXT_EPOCH_SCRIPT = new DefaultRedisScript<>("""
            local current = tonumber(redis.call('GET', KEYS[1]) or '0')
            local floor = tonumber(ARGV[1])
            if current < floor then redis.call('SET', KEYS[1], floor) end
            local next = redis.call('INCR', KEYS[1])
            redis.call('EXPIRE', KEYS[1], ARGV[2])
            return next
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final CourseQueryService courseQueryService;
    private final EntitlementService entitlementService;
    private final ProgressQueryService progressQueryService;

    public PlaybackSessionService(StringRedisTemplate redisTemplate, CourseQueryService courseQueryService,
                                  EntitlementService entitlementService, ProgressQueryService progressQueryService) {
        this.redisTemplate = redisTemplate;
        this.courseQueryService = courseQueryService;
        this.entitlementService = entitlementService;
        this.progressQueryService = progressQueryService;
    }

    public PlaybackSessionBO start(String userId, String videoId) {
        ChapterBO chapter = courseQueryService.requirePublishedVideo(videoId);
        entitlementService.requireActive(userId, chapter.courseId());
        VideoProgressBO progress = progressQueryService.get(userId, videoId);
        long floor = Math.max(progress.sessionEpoch(), System.currentTimeMillis() * 1000L);
        Long epoch = redisTemplate.execute(NEXT_EPOCH_SCRIPT, List.of(epochKey(userId, videoId)),
                String.valueOf(floor), String.valueOf(Duration.ofDays(30).toSeconds()));
        if (epoch == null) {
            throw BusinessException.serviceUnavailable("暂时无法创建播放会话");
        }
        String sessionId = UUID.randomUUID().toString();
        String key = sessionKey(userId, videoId);
        redisTemplate.opsForHash().putAll(key, Map.of(
                "sessionId", sessionId,
                "sessionEpoch", String.valueOf(epoch),
                "videoVersion", String.valueOf(chapter.videoVersion())));
        redisTemplate.expire(key, Duration.ofHours(2));
        return new PlaybackSessionBO(sessionId, epoch, videoId, chapter.videoVersion(),
                progress.resumePositionMs());
    }

    public void validate(String userId, String videoId, String sessionId, long epoch, int videoVersion) {
        Map<Object, Object> session = redisTemplate.opsForHash().entries(ProgressRedisKey.session(userId, videoId));
        if (!sessionId.equals(String.valueOf(session.get("sessionId")))
                || !String.valueOf(epoch).equals(String.valueOf(session.get("sessionEpoch")))
                || !String.valueOf(videoVersion).equals(String.valueOf(session.get("videoVersion")))) {
            throw BusinessException.conflict("播放会话已过期或已被新设备替代，请重新创建会话");
        }
        redisTemplate.expire(ProgressRedisKey.session(userId, videoId), Duration.ofHours(2));
    }

    private String epochKey(String userId, String videoId) {
        return ProgressRedisKey.sessionEpoch(userId, videoId);
    }

    private String sessionKey(String userId, String videoId) {
        return ProgressRedisKey.session(userId, videoId);
    }
}
