# 第 5.3 轮：赛季结算与历史榜单

## 接口与作业

- `POST /api/points/admin/seasons/{season}/settlement`：首次结算与失败重试使用同一入口，要求 ADMIN。
- `GET /api/points/admin/seasons/{season}`：返回 READY / FAILED / SETTLED、结算时间、快照数量及安全的失败原因。
- `GET /api/points/admin/seasons/{season}/ranking`：已结算 TOP1000 快照分页，每页最多100条，保持全局排名。未结算返回409。
- XXL-JOB `settlePointSeason`：结算上一自然季度，要求预先创建季度配置并部署流水分表。失败由调度平台记录，可人工重试同一接口。
- 原快照和归档作业仍保留兼容，但不能修改已经 SETTLED 的赛季；旧归档作业改为精确计算上一季度，不再通过减100天猜测。

## 状态与事务

READY / FAILED 可尝试结算，SETTLED 为终态。HTTP 结算以 `season` 为资源自然幂等键，没有可变请求参数。重复成功请求回读原状态、时间和数量，不重复生成快照。失败回滚后单独记录 FAILED，条件更新避免覆盖并发成功的 SETTLED。

结算在一个 MySQL 本地事务中锁定赛季行，检查季度已结束及待入账任务，按 `season_account.points DESC, user_id ASC` 生成确定性 TOP1000 排名，再写快照、归档标记与终态。Redis 不参加结算事务，也不是最终排名事实源。既有非最终快照会在该事务内替换；任一步失败则整体回滚。

积分入账和签到 Outbox 任务创建都在原事务中锁定相同赛季行；因此结算与新积分提交串行。待入账检查使用 `point_task.occurred_at` 时间索引及季度流水 `event_id` 唯一索引，包含 SENT / DEAD 等所有任务状态：发布成功不等于积分入账。V4 从既有事件 JSON 回填发生时间，新任务显式保存 UTC 时间。

没有长期持久化的 PROCESSING 状态：本批是有界 TOP1000 的同步本地事务，执行中由数据库行锁串行，崩溃自动回滚；查询看到上次已提交状态。未来异步长任务需要增加租约与 PROCESSING 状态，而不是照搬此实现。

## 晚到消息策略

SETTLED 后，已存在的季度流水事件仍按原幂等逻辑返回，不重复增加积分。新的晚到事件不能修改已结算账户，消费者先将原始事件与原因写入 `rejected_point_event`，成功提交后才正常返回供 MQ 确认；保存失败仍抛出异常供重试。重复晚到投递使用 event_id 主键去重。

这不是静默丢弃，也不是自动把积分挪到下一季度。记录保留供人工核查，本批未实现运营查询与补发入口。未来外部生产者仍可能发送此前未知的事件，本系统无法仅靠内部 Outbox 检查证明全世界消息已经到齐；此类事件按上述策略留存。归档 READ_ONLY 是业务写入限制和元数据标记，不是数据库权限变更，也没有物理迁移流水表。

## 验证入口

- `SeasonSettlementRuleTest`：结束边界可结算、提前结算拒绝、有积压任务拒绝。
- `PointsAcceptanceIT.concurrentSettlementUsesDatabaseRankingAndRetainsLateEvents`：两实例调用并发回放、Redis清空仍正确排名、重复消息不增分、晚到原事件留存、ADMIN权限、最终历史分页与旧作业保护。
- `PointsAcceptanceIT.outstandingPublishedTaskBlocksSettlementUntilConsumerAppliesIt`：已 SENT 但未入账仍阻止结算，消费后 FAILED 可安全重试。
- `PointsAcceptanceIT.settlementFailureRollsBackSnapshotAndArchiveAndCanRetry`：通过临时数据库 CHECK 约束注入归档失败，验证快照与归档整体回滚，恢复后安全重跑。
- OpenAPI 验证新增结算路径，原积分消费、重建、赛季配置验收继续回归。

## 执行结果

2026-09-14 执行通过：

`JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home mvn -Dmaven.repo.local=.m2/repository -Pacceptance verify`

全仓库 40 个单元测试、32 个集成验收测试通过，无失败、无跳过。其中积分模块 7 个单元测试、7 个 MySQL / Redis / RabbitMQ 容器验收场景通过。结果是正确性回归，不代表压测或生产容量承诺。
