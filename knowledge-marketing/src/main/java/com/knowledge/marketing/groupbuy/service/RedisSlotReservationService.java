package com.knowledge.marketing.groupbuy.service;

import com.knowledge.common.exception.BusinessException;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
public class RedisSlotReservationService implements SlotReservationService {

    private static final Duration RESERVATION_TTL = Duration.ofMinutes(30);
    private static final DefaultRedisScript<Long> RESERVE_SCRIPT = new DefaultRedisScript<>("""
            local current = redis.call('GET', KEYS[2])
            if current then
              if current == ARGV[1] then return 2 else return -2 end
            end
            if redis.call('EXISTS', KEYS[1]) == 0 then
              redis.call('SET', KEYS[1], ARGV[2], 'EX', ARGV[5], 'NX')
            end
            -- Keep the evidence longer than the business timeout so a delayed compensator can still identify the owner.
            redis.call('SET', KEYS[2], ARGV[1], 'EX', ARGV[5], 'NX')
            local occupied = redis.call('INCR', KEYS[1])
            redis.call('EXPIRE', KEYS[1], ARGV[5])
            if occupied > tonumber(ARGV[3]) then
              redis.call('DECR', KEYS[1])
              redis.call('DEL', KEYS[2])
              return 0
            end
            redis.call('ZADD', KEYS[3], ARGV[6], KEYS[2])
            redis.call('EXPIRE', KEYS[3], ARGV[5])
            return 1
            """, Long.class);
    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[2]) ~= ARGV[1] then return 0 end
            redis.call('DEL', KEYS[2])
            local occupied = tonumber(redis.call('GET', KEYS[1]) or '0')
            if occupied > 0 then redis.call('DECR', KEYS[1]) end
            redis.call('ZREM', KEYS[3], KEYS[2])
            return 1
            """, Long.class);
    private static final DefaultRedisScript<Long> CONFIRM_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[1]) ~= ARGV[1] then return 0 end
            redis.call('DEL', KEYS[1])
            redis.call('ZREM', KEYS[2], KEYS[1])
            return 1
            """, Long.class);

    private final StringRedisTemplate redisTemplate;

    public RedisSlotReservationService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public ReservationResult reserve(String groupId, String requestId, String userId,
                                     int knownOccupied, int capacity) {
        String counterKey = counterKey(groupId);
        String reservationKey = reservationKey(groupId, requestId);
        String expiryKey = expiryKey();
        long now = System.currentTimeMillis();
        Long result = redisTemplate.execute(RESERVE_SCRIPT, List.of(counterKey, reservationKey, expiryKey),
                userId, Integer.toString(knownOccupied), Integer.toString(capacity),
                Long.toString(RESERVATION_TTL.toSeconds()), Long.toString(Duration.ofDays(2).toSeconds()),
                Long.toString(now + RESERVATION_TTL.toMillis()));
        if (Long.valueOf(2).equals(result)) {
            return ReservationResult.IDEMPOTENT;
        }
        if (Long.valueOf(-2).equals(result)) {
            throw BusinessException.conflict("请求幂等键已被其他用户使用");
        }
        if (!Long.valueOf(1).equals(result)) {
            throw BusinessException.conflict("拼团名额已满");
        }
        return ReservationResult.ACQUIRED;
    }

    @Override
    public void release(String groupId, String requestId, String userId) {
        redisTemplate.execute(RELEASE_SCRIPT,
                List.of(counterKey(groupId), reservationKey(groupId, requestId), expiryKey()), userId);
    }

    @Override
    public void confirm(String groupId, String requestId, String userId) {
        redisTemplate.execute(CONFIRM_SCRIPT,
                List.of(reservationKey(groupId, requestId), expiryKey()), userId);
    }

    @Override
    public List<ExpiredReservation> findExpired(int limit) {
        Set<String> keys = redisTemplate.opsForZSet().rangeByScore(expiryKey(), 0, System.currentTimeMillis(), 0, limit);
        if (keys == null || keys.isEmpty()) {
            return List.of();
        }
        return keys.stream().map(key -> {
            String prefix = "kw:marketing:reservation:";
            if (!key.startsWith(prefix)) {
                return null;
            }
            String suffix = key.substring(prefix.length());
            int separator = suffix.indexOf(':');
            if (separator < 1) {
                return null;
            }
            String userId = redisTemplate.opsForValue().get(key);
            return userId == null ? null : new ExpiredReservation(
                    suffix.substring(0, separator), suffix.substring(separator + 1), userId);
        }).filter(java.util.Objects::nonNull).toList();
    }

    static String counterKey(String groupId) {
        return "kw:marketing:group:occupied:" + groupId;
    }

    static String reservationKey(String groupId, String requestId) {
        return "kw:marketing:reservation:" + groupId + ':' + requestId;
    }

    static String expiryKey() {
        return "kw:marketing:reservation-expiry";
    }
}
