# 第 5.1 轮学习主线验收记录

- 验收日期：2026-09-08
- 环境：JDK 17、MySQL 8.4、Redis 7.4、RabbitMQ 4.1（Testcontainers）
- 命令：`mvn -Dmaven.repo.local=.m2/repository -pl knowledge-learning -am verify -Pacceptance`

## 交付范围

本轮按三批交付，不拆分虚假的空微服务：

1. 课程、章节与 Note：管理员维护课程/章节；用户按 `clientRequestId` 幂等创建 Note，支持重命名、编辑、软删除、所有权校验和乐观锁。
2. 视频进度：播放会话、跨设备 epoch、会话内 sequence、RabbitMQ 持久消息、Inbox 去重、10 秒观看片段、完成度、Redis 快照和 MySQL 回源。
3. 课程权益：营销服务在成团事件中携带全部确认用户；学习服务以 Inbox 和业务唯一键幂等发放权益。

服务内部按 `course`、`note`、`progress`、`entitlement` 业务域分包，域内采用 `controller/service/dao/dto/bo/vo/converter` 传统分层。数据库实体使用 `DO` 后缀，且只位于 `dao/model`。

## 验收结果

| 场景 | 实际结果 |
|---|---|
| Note 重复创建、标题去空白、重命名、版本冲突、越权访问、无权益创建 | 通过 |
| 同一成团事件重复投递，用户列表自身重复 | 每个用户只获得一份权益，Inbox 只记录一次 |
| 同一进度事件重复上报 10 次 | 观看片段只累计一次，90% 覆盖率正确完成 |
| 乱序 sequence、当前会话回看、新设备替代旧设备 | 旧消息不覆盖新断点，合法回看可保存，旧设备被拒绝 |
| Redis 清空后读取断点 | 从 MySQL 恢复并重新填充 Redis |
| 成团/进度坏消息 | 有限重试后分别进入 `learning.group-formed.dlq` 与 `learning.video-progress.dlq` |
| OpenAPI、未认证访问、普通用户访问管理员接口 | 契约存在；分别返回 401 和 403 统一响应 |

结果：6 个 Testcontainers 集成场景全部通过，0 失败、0 错误。

## 一致性说明

- 营销侧订单、团状态和通知任务使用本地事务；通知任务负责 MQ 发送失败重试。
- 学习侧消费按至少一次投递设计，Inbox 与业务唯一键共同保证幂等。
- MQ 消费连续失败不会静默丢弃，进入 DLQ 等待人工检查或受控重放。
- 视频进度写请求只有在 publisher confirm 成功后才返回已接收；Redis 失败不影响已入队事实，消费者会修复缓存。

## 尚未包含

课程级完成度投影、视频防作弊、社区、扩展运营后台、Micrometer 指标和链路追踪不在第 5.1 轮范围，不能表述为已完成。
