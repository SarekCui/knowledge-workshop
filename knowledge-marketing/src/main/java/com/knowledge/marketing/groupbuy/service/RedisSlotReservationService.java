package com.knowledge.marketing.groupbuy.service;

import com.knowledge.common.exception.BusinessException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
public class RedisSlotReservationService implements SlotReservationService {

    private static final Duration RESERVATION_TTL = Duration.ofMinutes(30);
    private static final Duration EVIDENCE_TTL = Duration.ofDays(2);
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
            redis.call('ZADD', KEYS[3], ARGV[6], ARGV[1])
            redis.call('EXPIRE', KEYS[3], ARGV[5])
            return 1
            """, Long.class);
    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[2]) ~= ARGV[1] then return 0 end
            redis.call('DEL', KEYS[2])
            local occupied = tonumber(redis.call('GET', KEYS[1]) or '0')
            if occupied > 0 then redis.call('DECR', KEYS[1]) end
            redis.call('ZREM', KEYS[3], ARGV[1])
            return 1
            """, Long.class);
    private static final DefaultRedisScript<Long> CONFIRM_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[1]) ~= ARGV[1] then return 0 end
            redis.call('DEL', KEYS[1])
            redis.call('ZREM', KEYS[2], ARGV[1])
            return 1
            """, Long.class);

    private final StringRedisTemplate redisTemplate;

    public RedisSlotReservationService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public ReservationResult reserve(String groupId, String userId,
                                     int knownOccupied, int capacity) {
        String counterKey = GroupBuyRedisKey.occupied(groupId);
        String reservationKey = GroupBuyRedisKey.reservation(groupId, userId);
        String expiryKey = GroupBuyRedisKey.reservationExpiry(groupId);
        long now = System.currentTimeMillis();
        Long result = redisTemplate.execute(RESERVE_SCRIPT, List.of(counterKey, reservationKey, expiryKey),
                userId, Integer.toString(knownOccupied), Integer.toString(capacity),
                Long.toString(RESERVATION_TTL.toSeconds()), Long.toString(EVIDENCE_TTL.toSeconds()),
                Long.toString(now + RESERVATION_TTL.toMillis()));
        if (Long.valueOf(1).equals(result) || Long.valueOf(2).equals(result)) {
            try {
                redisTemplate.opsForSet().add(GroupBuyRedisKey.EXPIRY_GROUP_REGISTRY, groupId);
                redisTemplate.expire(GroupBuyRedisKey.EXPIRY_GROUP_REGISTRY, EVIDENCE_TTL);
            } catch (RuntimeException exception) {
                if (Long.valueOf(1).equals(result)) {
                    redisTemplate.execute(RELEASE_SCRIPT, List.of(counterKey, reservationKey, expiryKey), userId);
                }
                throw exception;
            }
        }
        if (Long.valueOf(2).equals(result)) {
            return ReservationResult.IDEMPOTENT;
        }
        if (!Long.valueOf(1).equals(result)) {
            throw BusinessException.conflict("拼团名额已满");
        }
        return ReservationResult.ACQUIRED;
    }

    @Override
    public void release(String groupId, String userId) {
        redisTemplate.execute(RELEASE_SCRIPT,
                List.of(GroupBuyRedisKey.occupied(groupId), GroupBuyRedisKey.reservation(groupId, userId),
                        GroupBuyRedisKey.reservationExpiry(groupId)), userId);
    }

    @Override
    public void confirm(String groupId, String userId) {
        redisTemplate.execute(CONFIRM_SCRIPT,
                List.of(GroupBuyRedisKey.reservation(groupId, userId),
                        GroupBuyRedisKey.reservationExpiry(groupId)), userId);
    }

    @Override
    public List<ExpiredReservation> findExpired(int limit) {
        if (limit <= 0) {
            return List.of();
        }
        List<ExpiredReservation> expired = new ArrayList<>(limit);
        ScanOptions options = ScanOptions.scanOptions().count(Math.min(limit, 100)).build();
        try (Cursor<String> groups = redisTemplate.opsForSet()
                .scan(GroupBuyRedisKey.EXPIRY_GROUP_REGISTRY, options)) {
            while (groups.hasNext() && expired.size() < limit) {
                String groupId = groups.next();
                int remaining = limit - expired.size();
                Set<String> userIds = redisTemplate.opsForZSet().rangeByScore(
                        GroupBuyRedisKey.reservationExpiry(groupId), 0, System.currentTimeMillis(), 0, remaining);
                if (userIds != null) {
                    userIds.forEach(userId -> expired.add(new ExpiredReservation(groupId, userId)));
                }
            }
        }
        return expired;
    }
}
