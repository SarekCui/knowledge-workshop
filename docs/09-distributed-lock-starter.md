# 分布式锁 Starter 设计

## 1. 模块定位

模块名为 `distributed-lock-spring-boot-starter`，提供基于 Spring AOP 与 Redisson 的声明式分布式锁能力。模块只负责进程间互斥，不替代数据库唯一约束、条件更新、事务和业务幂等。

包名保持 `com.knowledge.lock`。`knowledge` 是项目命名空间，`lock` 是组件能力；实现相关类型使用 `Redisson` 前缀，避免把具体实现伪装成通用抽象。

## 2. 对外注解

```java
@DistributedLock(
        keys = {
                "'kw:marketing:lock:activity:' + #activityId",
                "'kw:marketing:lock:user:' + #userId"
        },
        lockType = LockType.REENTRANT,
        waitTime = 3,
        leaseTime = 10,
        timeUnit = TimeUnit.SECONDS,
        failurePolicy = LockFailurePolicy.THROW_EXCEPTION)
public void join(String activityId, String userId) {
    // 临界区内只执行必要的本地操作
}
```

- `keys`：一个或多个 SpEL 表达式。解析后去重，并按字典序获取，避免不同调用顺序形成循环等待。
- `lockType`：支持可重入锁、公平锁、读锁和写锁。
- `waitTime`：获取全部锁共享的总等待预算，必须大于等于 0。
- `leaseTime`：正数表示固定租约；`-1` 使用 Redisson 看门狗自动续期；不允许 0 或小于 -1。
- `failurePolicy`：`THROW_EXCEPTION` 抛出获取锁异常；`RETURN_NULL` 跳过方法执行并返回 `null`。

`RETURN_NULL` 不能用于返回基本类型的方法。线程中断始终恢复中断标记并抛出异常，不受失败策略影响。

## 3. 内部职责

```text
DistributedLockAspect
  -> LockKeyResolver / SpelLockKeyResolver
  -> LockOptions
  -> LockExecutionStrategyResolver
       -> SingleLockExecutionStrategy
       -> OrderedMultiLockExecutionStrategy
  -> RedissonLockFactory
```

- `DistributedLockAspect`：读取注解、校验返回类型、组织调用，不实现具体锁算法。
- `LockKeyResolver`：解析锁 Key，允许业务应用替换默认 SpEL 实现。
- `LockOptions`：保存已解析且校验后的锁执行参数，不是 HTTP DTO。
- `LockExecutionStrategyResolver`：根据唯一 Key 数量选择单锁或有序多锁策略。
- `RedissonLockFactory`：根据 `LockType` 创建 Redisson `RLock`。
- `LockReleaseSupport`：逆序释放当前线程持有的锁；解锁失败不会覆盖原始业务异常。

## 4. 失败与并发语义

1. 多把锁先去重再排序，调用方声明顺序不影响实际获取顺序。
2. `waitTime` 是获取所有锁的总预算，不会为每把锁重新计算完整等待时间。
3. 第二把或后续锁获取失败时，已经获取的锁按逆序释放。
4. 只在 `isHeldByCurrentThread()` 为真时解锁。
5. 业务异常原样抛出；解锁异常作为 suppressed exception 附加，不覆盖根因。
6. 获取锁的线程被中断时恢复中断标记，并以 `INTERRUPTED` 原因抛出。

## 5. 参考项目取舍

- 参考 `axing100/redisson-distributed-lock` 的锁类型、阻塞/尝试获取语义和看门狗租约设计，但不采用静态持有 `RedissonClient` 的工具类，也不在切面中重复四套锁分支。
- 参考 `baomidou/lock4j` 的锁模板、Key 构建和底层执行器职责分离，但当前组件只支持 Redisson，不提前抽象 RedisTemplate、Zookeeper 等未使用实现。
- 保留本项目独有的有序多锁和共享等待预算；lock4j 的多个 `keys` 默认拼接为一个 Key，与本组件的多锁语义不同。

参考地址：

- <https://github.com/axing100/redisson-distributed-lock>
- <https://github.com/baomidou/lock4j>

## 6. 使用约束

- 锁 Key 遵循 `kw:{service}:lock:{purpose}:{businessIdentifiers}`，由所属业务域集中定义 SpEL 模板。
- 持锁期间禁止慢速远程调用和无界循环。
- 锁只能降低并发冲突，关键写入仍需数据库唯一索引或条件更新兜底。
- 固定租约必须覆盖临界区合理最长耗时；耗时不可预测时使用 `-1` 看门狗并保证方法最终退出。
