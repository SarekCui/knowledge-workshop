package com.knowledge.lock.annotation;

import com.knowledge.lock.enums.LockFailurePolicy;
import com.knowledge.lock.enums.LockType;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 为方法声明一个或多个按固定顺序获取的分布式锁。
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {

    /** SpEL 锁 Key；多个 Key 会去重、排序后依次获取。 */
    String[] keys();

    /** Redisson 锁类型。 */
    LockType lockType() default LockType.REENTRANT;

    /** 获取全部锁共享的最长等待时间。 */
    long waitTime() default 3;

    /** 锁租约；-1 表示使用 Redisson 看门狗自动续期。 */
    long leaseTime() default -1;

    /** 等待时间和租约的单位。 */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /** 获取锁超时后的处理方式；线程中断始终向上抛出。 */
    LockFailurePolicy failurePolicy() default LockFailurePolicy.THROW_EXCEPTION;
}
