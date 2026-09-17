package com.knowledge.lock.core;

import com.knowledge.lock.exception.LockAcquisitionException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.redisson.api.RLock;

public class OrderedMultiLockExecutionStrategy implements LockExecutionStrategy {

    private final RedissonLockFactory lockFactory;

    public OrderedMultiLockExecutionStrategy(RedissonLockFactory lockFactory) {
        this.lockFactory = lockFactory;
    }

    @Override
    public boolean supports(LockOptions options) {
        return options.keys().size() > 1;
    }

    @Override
    public <T> T execute(LockOptions options, LockCallback<T> callback) throws Throwable {
        List<String> sortedKeys = options.keys().stream().distinct().sorted(Comparator.naturalOrder()).toList();
        List<RLock> acquiredLocks = new ArrayList<>(sortedKeys.size());
        long deadlineNanos = System.nanoTime() + options.timeUnit().toNanos(options.waitTime());
        Throwable executionFailure = null;
        try {
            for (String key : sortedKeys) {
                RLock lock = lockFactory.create(key, options.lockType());
                long remainingNanos = Math.max(0, deadlineNanos - System.nanoTime());
                boolean acquired = lock.tryLock(remainingNanos, options.timeUnit().toNanos(options.leaseTime()),
                        java.util.concurrent.TimeUnit.NANOSECONDS);
                if (!acquired) {
                    throw LockAcquisitionException.timeout("获取组合分布式锁超时: " + key);
                }
                acquiredLocks.add(lock);
            }
            return callback.execute();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            LockAcquisitionException lockException =
                    LockAcquisitionException.interrupted("获取组合分布式锁时线程被中断", exception);
            executionFailure = lockException;
            throw lockException;
        } catch (Throwable exception) {
            executionFailure = exception;
            throw exception;
        } finally {
            LockReleaseSupport.releaseAll(acquiredLocks, executionFailure);
        }
    }
}
