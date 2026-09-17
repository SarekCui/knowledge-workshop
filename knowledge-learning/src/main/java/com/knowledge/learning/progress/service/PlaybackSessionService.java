package com.knowledge.learning.progress.service;

import com.knowledge.common.exception.BusinessException;
import com.knowledge.learning.course.bo.ChapterBO;
import com.knowledge.learning.course.service.CourseQueryService;
import com.knowledge.learning.entitlement.service.EntitlementService;
import com.knowledge.learning.progress.bo.PlaybackSessionBO;
import com.knowledge.learning.progress.bo.VideoProgressBO;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataAccessException;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class PlaybackSessionService {

    private static final DefaultRedisScript<Long> CREATE_SESSION_SCRIPT = new DefaultRedisScript<>("""
            local current = tonumber(redis.call('GET', KEYS[1]) or '0')
            local floor = tonumber(ARGV[1])
            if current < floor then redis.call('SET', KEYS[1], floor) end
            local next = redis.call('INCR', KEYS[1])
            redis.call('EXPIRE', KEYS[1], ARGV[2])
            redis.call('HSET', KEYS[2], 'sessionId', ARGV[3],
              'sessionEpoch', redis.call('GET', KEYS[1]), 'videoVersion', ARGV[4])
            redis.call('EXPIRE', KEYS[2], ARGV[5])
            return next
            """, Long.class);
    private static final DefaultRedisScript<Long> VALIDATE_SESSION_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('HGET', KEYS[1], 'sessionId') ~= ARGV[1]
              or redis.call('HGET', KEYS[1], 'sessionEpoch') ~= ARGV[2]
              or redis.call('HGET', KEYS[1], 'videoVersion') ~= ARGV[3] then
              return 0
            end
            redis.call('EXPIRE', KEYS[1], ARGV[4])
            return 1
            """, Long.class);
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private CourseQueryService courseQueryService;
    @Autowired
    private EntitlementService entitlementService;
    @Autowired
    private ProgressQueryService progressQueryService;

    public PlaybackSessionBO start(String userId, String videoId) {
        ChapterBO chapter = courseQueryService.requirePublishedVideo(videoId);
        entitlementService.requireActive(userId, chapter.courseId());
        VideoProgressBO progress = progressQueryService.get(userId, videoId);
        long floor = Math.max(progress.sessionEpoch(), System.currentTimeMillis() * 1000L);
        String sessionId = UUID.randomUUID().toString();
        Long epoch;
        try {
            epoch = redisTemplate.execute(CREATE_SESSION_SCRIPT,
                    List.of(epochKey(userId, videoId), sessionKey(userId, videoId)),
                    String.valueOf(floor), String.valueOf(Duration.ofDays(30).toSeconds()), sessionId,
                    String.valueOf(chapter.videoVersion()), String.valueOf(Duration.ofHours(2).toSeconds()));
        } catch (DataAccessException redisFailure) {
            throw BusinessException.serviceUnavailable("播放会话服务暂不可用，请稍后重试");
        }
        if (epoch == null) {
            throw BusinessException.serviceUnavailable("暂时无法创建播放会话");
        }
        return new PlaybackSessionBO(sessionId, epoch, videoId, chapter.videoVersion(),
                progress.resumePositionMs());
    }

    public void validate(String userId, String videoId, String sessionId, long epoch, int videoVersion) {
        Long valid;
        try {
            valid = redisTemplate.execute(VALIDATE_SESSION_SCRIPT, List.of(sessionKey(userId, videoId)),
                    sessionId, String.valueOf(epoch), String.valueOf(videoVersion),
                    String.valueOf(Duration.ofHours(2).toSeconds()));
        } catch (DataAccessException redisFailure) {
            throw BusinessException.serviceUnavailable("播放会话服务暂不可用，请稍后重试");
        }
        if (valid == null) {
            throw BusinessException.serviceUnavailable("暂时无法校验播放会话");
        }
        if (valid != 1) {
            throw BusinessException.conflict("播放会话已过期或已被新设备替代，请重新创建会话");
        }
    }

    private String epochKey(String userId, String videoId) {
        return ProgressRedisKey.sessionEpoch(userId, videoId);
    }

    private String sessionKey(String userId, String videoId) {
        return ProgressRedisKey.session(userId, videoId);
    }
}
