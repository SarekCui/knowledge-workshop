# 第 2 轮验收记录：知识付费拼团

- Git commit：当前目录尚未初始化 Git，暂记为 `N/A`
- 验收日期：2026-09-03
- 环境：Apple Silicon、macOS 26.5.2、JDK 17.0.16、Docker 28.3.0
- 隔离依赖：Testcontainers 1.21.4、MySQL 8.4、Redis 7.4、RabbitMQ 4.1

## 验收命令

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home \
PATH=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home/bin:$PATH \
mvn -Dmaven.repo.local=.m2/repository -pl knowledge-marketing -am verify -Pacceptance
```

## 验收项

| 场景 | 预期 | 实际 | 自动化证据 | 结果 |
|---|---|---|---|---|
| 100 个独立请求竞争 10 个名额 | 仅 10 个请求成功，数据库确认人数不超过 10 | 成功 10，拒绝 90；参与记录、订单、团确认人数均为 10，Redis 占用计数为 10 | `oneHundredConcurrentRequestsCompeteForTenSlotsWithoutOverselling` | 通过 |
| 相同请求重复提交 | 返回同一订单，不重复写入 | 两次调用返回同一订单 ID，数据库仅 1 条订单 | `duplicateRequestIsIdempotentAndDatabaseFailureReleasesRedisSlot` | 通过 |
| 数据库唯一键冲突导致事务失败 | 事务回滚并释放刚取得的 Redis 名额 | 失败请求无订单，Redis 计数回到失败前数值 | `duplicateRequestIsIdempotentAndDatabaseFailureReleasesRedisSlot` | 通过 |
| Redis 不可用 | 数据库写事务不得开始 | Redis 异常直接向上返回，交易写服务调用次数为 0 | `redisFailureStopsTheWriteBeforeOpeningTheDatabaseTransaction` | 通过 |
| RabbitMQ 交换机短暂不可用 | 可靠任务进入重试；恢复后可发送且标记成功 | 首次为 `RETRY` 且重试数为 1，恢复交换机后为 `SENT` | `failedNotificationIsRetriedAndEventuallyMarkedSent` | 通过 |
| 空库迁移 | Flyway 可初始化全部营销表 | Testcontainers 新建空库并成功执行 V1 | `MarketingAcceptanceIT` 启动日志 | 通过 |

## 结果边界

本轮验证的是固定并发量下的正确性和故障恢复，不是持续性能压测，因此不据此宣称 100 RPS、P95 或生产容量。进程强杀、网络延迟注入和长时间稳定性测试保留到第 6 轮故障演练。

## 是否允许进入下一轮

允许。第 2 轮约定的核心并发、幂等、事务回补与可靠通知场景已有可重复自动化证据。
