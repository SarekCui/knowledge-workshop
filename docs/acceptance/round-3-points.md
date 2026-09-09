# 第 3 轮验收记录：积分赛季排名

- Git commit：当前目录尚未初始化 Git，暂记为 `N/A`
- 验收日期：2026-09-03
- 环境：Apple Silicon、macOS 26.5.2、JDK 17.0.16、Docker 28.3.0
- 隔离依赖：Testcontainers 1.21.4、MySQL 8.4、Redis 7.4、RabbitMQ 4.1

## 验收命令

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home \
PATH=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home/bin:$PATH \
mvn -Dmaven.repo.local=.m2/repository -pl knowledge-points -am verify -Pacceptance
```

## 验收项

| 场景 | 预期 | 实际 | 自动化证据 | 结果 |
|---|---|---|---|---|
| 同一 RabbitMQ 积分消息投递 10 次 | 只产生一条积分流水，只增加一次账户积分 | 流水 1 条；总账户与赛季账户均为 20 分 | `tenDuplicateRabbitMessagesCreateOneLedgerEntry` | 通过 |
| Redis 榜单丢失 | 从季度积分事实流水恢复 | 删除正式 ZSet 后聚合 Q3 流水，恢复 2 名用户且分数、顺序正确 | `leaderboardCanBeRebuiltAndSeasonJobsAreIdempotent` | 通过 |
| 榜单恢复过程 | 不向读请求暴露半成品榜单 | 先写临时 ZSet，再通过 Redis rename 原子替换正式 Key | `LeaderboardService.replace` | 通过 |
| 赛季快照重复执行 | 只生成一份快照 | 首次写入 2 条，第二次返回 0，数据库保持 2 条 | `leaderboardCanBeRebuiltAndSeasonJobsAreIdempotent` | 通过 |
| 赛季归档重复执行 | 只生成一条归档记录 | 首次成功，第二次返回 false，数据库保持 1 条 | `leaderboardCanBeRebuiltAndSeasonJobsAreIdempotent` | 通过 |
| 空库迁移 | Flyway 可初始化积分表和季度物理表 | Testcontainers 新建空库并成功执行 V1 | `PointsAcceptanceIT` 启动日志 | 通过 |

## 恢复实现

`LeaderboardRebuildService` 只接受 `QuarterTableRouter` 生成的受控表名，按用户聚合季度流水。`LeaderboardService` 将结果写入带 TTL 的临时榜单，完成后原子替换正式榜单，避免恢复期间读到部分数据。数据库流水仍是事实源，Redis 只承担查询加速。

## 结果边界

本轮证明了至少一次消息语义下的业务幂等、缓存可恢复以及任务重复执行安全性。尚未进行 RabbitMQ 大规模积压、Redis 节点切换和多实例同时触发 XXL-JOB 的性能/故障演练，这些属于第 6 轮。

## 是否允许进入下一轮

允许。第 3 轮约定的重复消息、榜单恢复、快照与归档幂等均已有真实中间件自动化证据。
