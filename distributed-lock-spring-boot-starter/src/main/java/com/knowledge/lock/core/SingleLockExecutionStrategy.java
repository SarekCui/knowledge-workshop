package com.knowledge.lock.core;

import com.knowledge.lock.exception.LockAcquisitionException;
import org.redisson.api.RLock;

public class SingleLockExecutionStrategy implements LockExecutionStrategy {

    private final RedissonLockFactory lockFactory;

    public SingleLockExecutionStrategy(RedissonLockFactory lockFactory) {
        this.lockFactory = lockFactory;
    }

    @Override
    public boolean supports(LockOptions options) {
        return options.keys().size() == 1;
    }

    @Override
    public <T> T execute(LockOptions options, LockCallback<T> callback) throws Throwable {
        RLock lock = lockFactory.create(options.keys().get(0), options.lockType());
        boolean acquired;
        try {
            acquired = lock.tryLock(options.waitTime(), options.leaseTime(), options.timeUnit());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw LockAcquisitionException.interrupted("获取分布式锁时线程被中断", exception);
        }
        if (!acquired) {
            throw LockAcquisitionException.timeout("获取分布式锁超时: " + options.keys().get(0));
        }
        Throwable businessFailure = null;
        try {
            return callback.execute();
        } catch (Throwable exception) {
            businessFailure = exception;
            throw exception;
        } finally {
            LockReleaseSupport.release(lock, businessFailure);
        }
    }
}
