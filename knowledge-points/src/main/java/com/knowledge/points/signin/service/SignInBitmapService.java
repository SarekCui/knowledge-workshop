package com.knowledge.points.signin.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class SignInBitmapService {

    private static final Duration BITMAP_RETENTION = Duration.ofDays(550);
    private final StringRedisTemplate redisTemplate;

    public SignInBitmapService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean markSigned(String userId, LocalDate date) {
        String key = key(userId, YearMonth.from(date));
        Boolean previous = redisTemplate.opsForValue().setBit(key,
                date.getDayOfMonth() - 1L, true);
        redisTemplate.expire(key, BITMAP_RETENTION);
        return Boolean.TRUE.equals(previous);
    }

    public void clear(String userId, LocalDate date) {
        redisTemplate.opsForValue().setBit(key(userId, YearMonth.from(date)), date.getDayOfMonth() - 1L, false);
    }

    public int continuousDays(String userId, LocalDate date) {
        int days = 0;
        LocalDate cursor = date;
        while (days < 366 && Boolean.TRUE.equals(redisTemplate.opsForValue().getBit(
                key(userId, YearMonth.from(cursor)), cursor.getDayOfMonth() - 1L))) {
            days++;
            cursor = cursor.minusDays(1);
        }
        return days;
    }

    private String key(String userId, YearMonth month) {
        return SignInRedisKey.bitmap(userId, month);
    }
}
