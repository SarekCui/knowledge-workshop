package com.knowledge.lock.aspect;

import com.knowledge.lock.annotation.DistributedLock;
import com.knowledge.lock.core.LockExecutionStrategyResolver;
import com.knowledge.lock.core.LockOptions;
import com.knowledge.lock.enums.LockFailurePolicy;
import com.knowledge.lock.exception.LockAcquisitionException;
import com.knowledge.lock.key.LockKeyResolver;
import java.lang.reflect.Method;
import java.util.List;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

@Aspect
public class DistributedLockAspect {

    private final LockExecutionStrategyResolver strategyResolver;
    private final LockKeyResolver lockKeyResolver;

    public DistributedLockAspect(LockExecutionStrategyResolver strategyResolver, LockKeyResolver lockKeyResolver) {
        this.strategyResolver = strategyResolver;
        this.lockKeyResolver = lockKeyResolver;
    }

    @Around("@annotation(annotation)")
    public Object around(ProceedingJoinPoint joinPoint, DistributedLock annotation) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        if (annotation.failurePolicy() == LockFailurePolicy.RETURN_NULL
                && method.getReturnType().isPrimitive() && method.getReturnType() != Void.TYPE) {
            throw new IllegalStateException("RETURN_NULL 失败策略不能用于返回基本类型的方法: " + method);
        }
        List<String> keys = lockKeyResolver.resolve(annotation.keys(), joinPoint.getTarget(), method,
                joinPoint.getArgs());
        LockOptions options = new LockOptions(keys, annotation.lockType(), annotation.waitTime(),
                annotation.leaseTime(), annotation.timeUnit());
        try {
            return strategyResolver.resolve(options).execute(options, joinPoint::proceed);
        } catch (LockAcquisitionException exception) {
            if (exception.getReason() == LockAcquisitionException.Reason.TIMEOUT
                    && annotation.failurePolicy() == LockFailurePolicy.RETURN_NULL) {
                return null;
            }
            throw exception;
        }
    }
}
