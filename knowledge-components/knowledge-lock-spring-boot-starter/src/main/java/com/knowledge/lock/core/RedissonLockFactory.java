package com.knowledge.lock.core;

import com.knowledge.lock.enums.LockType;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

public class RedissonLockFactory {

    private final RedissonClient redissonClient;

    public RedissonLockFactory(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    public RLock create(String key, LockType type) {
        return switch (type) {
            case REENTRANT -> redissonClient.getLock(key);
            case FAIR -> redissonClient.getFairLock(key);
            case READ -> redissonClient.getReadWriteLock(key).readLock();
            case WRITE -> redissonClient.getReadWriteLock(key).writeLock();
        };
    }
}
