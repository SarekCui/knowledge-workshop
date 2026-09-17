package com.knowledge.lock.core;

import java.util.List;
import org.redisson.api.RLock;

final class LockReleaseSupport {

    private LockReleaseSupport() {
    }

    static void release(RLock lock, Throwable primaryFailure) {
        releaseAll(List.of(lock), primaryFailure);
    }

    static void releaseAll(List<RLock> locks, Throwable primaryFailure) {
        RuntimeException releaseFailure = null;
        for (int index = locks.size() - 1; index >= 0; index--) {
            try {
                RLock lock = locks.get(index);
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            } catch (RuntimeException exception) {
                if (primaryFailure != null) {
                    primaryFailure.addSuppressed(exception);
                } else if (releaseFailure == null) {
                    releaseFailure = exception;
                } else {
                    releaseFailure.addSuppressed(exception);
                }
            }
        }
        if (primaryFailure == null && releaseFailure != null) {
            throw releaseFailure;
        }
    }
}
