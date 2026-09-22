# 小智 Agent 契约与原型实施计划

- 状态：开发中（第一阶段无 RAG 问答原型已完成，未通过完整验收）
- 对应轮次：5.8
- 相关决策：ADR-0014、ADR-0015

## 1. 已确定的技术与治理边界

| 项目 | 首期决策 |
|---|---|
| 生成模型 | DeepSeek `deepseek-flash`，OpenAI 兼容 Chat Completions，非思考模式 |
| Agent 框架 | LangChain4j；仅 `model` 域依赖其类型 |
| Embedding | 阿里云百炼 `text-embedding-v4`，1024 维；`document` / `query` 区分索引与提问 |
| 向量检索 | Qdrant；一个公共资料 collection，稠密向量 TopK + metadata 过滤 |
| 事实源 | learning 管理课程、Note、评论、进度；agent_db 管理会话、运行、审计、引用和消费幂等 |
| 运行分发 | MySQL 持久化 Run 状态机 + Agent 执行 Outbox + RabbitMQ Quorum 执行队列 |
| 运行所有权 | 数据库条件抢占 + Lease + `executionVersion` Fencing Token；不使用 Redis 锁决定所有权 |
| 索引范围 | 已发布课程、章节、PUBLIC Note、平台帮助文档；不索引私人数据 |
| 预算 | 6,000 输入 Token、1,200 输出 Token、3 次只读工具调用、0.05 元/次、2 元/用户/日 |
| 留存 | 消息 90 天；运行/引用/用量 180 天；工具摘要 30 天；删除会话时即时删除消息与引用 |

生成模型调用只携带本轮问题、已授权检索片段及必要工具结果；私人资料不写入 Qdrant，只有用户主动在私人问答场景发起时才经受控工具按需读取。向供应商传递的用户标识使用不可逆内部匿名 ID，不使用用户名、邮箱或手机号。

## 2. learning 内部契约

Agent 使用服务间认证调用 `knowledge-learning`，调用方身份、用户身份和场景均由服务端建立；模型工具参数不能携带或覆盖用户身份。

| 能力 | 内部接口 | 规则 |
|---|---|---|
| 公共资料复核 | `POST /internal/agent/public-resources/resolve` | 输入来源 ID 和版本，learning 仅返回当前仍为 PUBLIC 且未删除的片段与可引用标题/链接 |
| 私人学习上下文 | `GET /internal/agent/users/{userId}/learning-context` | 仅返回调用用户已获权益课程、当前章节和进度摘要；不返回订单或积分 |
| 私人 Note | `POST /internal/agent/users/{userId}/notes/query` | 仅查询调用用户自己的 Note，分页有界，默认只返回标题和命中片段 |
| 公开评论上下文 | `GET /internal/notes/{noteId}/comments/{commentId}/agent-context` | 仅公开 Note；返回笔记标题、正文与来源评论 |
| AI 回复发布 | `POST /internal/notes/{noteId}/comments` | 与用户评论复用 `CreateNoteCommentDTO`；`parentCommentId` 为来源评论，learning 从服务身份确定 AI 作者并校验 Note 仍公开 |

Agent 使用 OpenFeign 经 Nacos 服务名调用 learning；Client 只声明上述内部契约，`LearningCommentService` 负责结果解包和调用编排。Feign 请求拦截器从 IAM 的标准 Client Credentials 端点获取并缓存 5 分钟 RS256 服务令牌，到期前 30 秒刷新；令牌限定 `aud=knowledge-learning`，读取与写回分别要求 `learning.comment.read`、`learning.comment.write` scope。连接/读取超时默认分别为 3/5 秒且不做透明重试；运行状态机负责有界恢复。Agent 无论检索来源为何，都在生成前调用公共资料复核接口；复核失败的片段不得进入 Prompt 或引用。

## 3. `AgentMentioned` 事件与可靠性

learning 在写入公开评论的同一 MySQL 事务中写 Outbox。事件只携带 ID，不携带 Note 正文或评论正文：

```json
{
  "eventId": "UUID",
  "eventType": "AgentMentioned",
  "occurredAt": "2026-09-17T00:00:00Z",
  "aggregateId": "commentId",
  "version": 1,
  "noteId": "noteId",
  "commentId": "commentId",
  "parentCommentId": "optional",
  "userId": "userId"
}
```

Agent 接收 `AgentMentioned` 后执行以下本地事务：

1. 以 `(eventId, consumer)` 写 Inbox 去重；
2. 以 `sourceCommentId` 唯一约束创建 `agent_run`；
3. 写 `agent_execution_outbox`，准备投递 `ExecuteRun` 命令；
4. 提交成功后 ACK `AgentMentioned`，不得在该消费事务中调用模型。

执行 Outbox 通过 Publisher Confirm 投递到独立 RabbitMQ 执行队列。生产队列采用 Quorum Queue、持久化消息、手动 ACK、有限 Prefetch 和 DLQ；当前 RabbitMQ 4.1 使用 TTL 重试队列与 DLX 实现延迟退避。Worker 收到执行消息后仍需通过数据库条件更新抢占运行：

执行消息只携带稳定标识，不携带 Prompt、上下文或模型回答：

```json
{
  "commandId": "UUID",
  "createdAt": "2026-09-18T00:00:00Z",
  "runId": "UUID",
  "runType": "COMMENT_REPLY"
}
```

建议使用独立命令交换机和队列，避免把内部执行命令混入跨服务领域事件：

```text
exchange: knowledge.agent.commands
routing:  agent.run.requested.v1
queue:    agent.run.execute.v1
retry:    agent.run.execute.retry.{10s,60s,300s}.v1
dead:     agent.run.execute.dlq.v1
```

```sql
UPDATE agent_run
   SET status = 'RUNNING',
       lease_owner = :instanceId,
       lease_until = :leaseUntil,
       execution_version = execution_version + 1
 WHERE id = :runId
   AND status IN ('PENDING', 'RETRY_WAIT')
   AND next_retry_at <= :now;
```

受影响行数为 `1` 才能继续执行；所有后续阶段更新必须携带抢占得到的 `executionVersion`，防止租约失效后的旧 Worker 覆盖新结果。

评论运行的生命周期状态与执行阶段分开保存：

```text
status: PENDING -> RUNNING -> SUCCEEDED
                  -> RETRY_WAIT -> RUNNING
                  -> DEAD / CANCELLED / TIMED_OUT

stage:  CONTEXT -> GENERATE -> PUBLISH
```

`agent_run` 至少保存 `status`、`run_stage`、`lease_owner`、`lease_until`、`execution_version`、`attempt_count`、`next_retry_at`、`answer`、`last_error_code`、`last_error_summary` 及各阶段时间。`last_error_summary` 必须脱敏，不保存完整 Prompt、令牌或供应商响应体。

模型回答在 `GENERATE -> PUBLISH` 阶段迁移时先持久化，再使用 `agent:comment-reply:{sourceCommentId}` 幂等键发布。发布失败保留 `PUBLISH` 阶段，只重试发布，不得重新调用模型；learning 继续以来源评论唯一索引兜底。资源已下架、权限失败和非法输入不重试；网络、限流等可恢复错误有界退避，达到配置的最大尝试次数（默认 5）进入 `DEAD`，并由后续 DLQ 重放能力统一处置。低频 Reconciliation Job 恢复租约过期、长期未投递和已有回答但未发布的运行。原评论始终保持发布成功。

## 4. 右侧 SSE 契约

```text
POST /api/agent/conversations
POST /api/agent/conversations/{conversationId}/messages/stream
POST /api/agent/runs/{runId}/cancel
```

消息请求含客户端生成的 `idempotencyKey`、问题和页面上下文提示。键格式为
`调用方:操作:稳定标识`，右侧问答使用 `web:chat-send:{uuid}`，同一逻辑提问重试时原样复用。相同会话同时只允许一个运行；冲突返回 409。SSE 仅输出以下业务事件，不透传模型协议和思维链：

```text
run.started       { runId }
answer.delta      { text }
citation.added    { sourceType, sourceId, sourceVersion, title, url }
tool.started      { toolName }
tool.completed    { toolName, durationMs }
answer.completed  { inputTokens, outputTokens }
answer.failed     { code, retryable }
```

客户端断开只断开 SSE 传输，不取消运行；用户显式调用取消接口或 30 秒模型总超时才将运行标记为取消/超时。超时运行不自动再次生成，避免重复计费。

### 4.1 当前学习端落地范围

- 全局右下角入口打开右侧“小智”抽屉；未登录用户只能看到登录引导。
- 登录用户可创建会话，携带当前路由作为页面上下文，并以 `fetch` 读取 SSE 后渲染 `run.started`、`answer.delta`、`answer.completed` 和 `answer.failed`。
- SSE 不走通用 JSON 请求封装，避免 15 秒超时和错误地解析流式响应；鉴权令牌续期后仅在首次响应为 401 时重新发起请求。
- 当前已实现会话和消息持久化、最近会话列表、历史消息读取与会话删除；这些历史目前仅用于界面展示，尚未构造为模型的有界 Chat Memory。
- 当前尚未实现显式取消、SSE 断线恢复、运行结果查询、引用展示或来源跳转。因服务端尚未实现这些契约，前端不得伪造相应入口。
- 前端状态层覆盖流式拼接、模型失败和跨分帧 SSE 解析；生产构建通过不等同于网关至模型的端到端验收。
- 本地一键启动必须包含 `knowledge-agent`（8087）并在启动 Agent 时启用 `local` Profile；否则旧网关 JAR 或未注册的 Agent 会使 `/api/agent/**` 返回 404/服务不可用，不能据此判断模型调用失败。

## 5. 实施顺序与验收门槛

1. 修复评论上下文接口的 PUBLIC、删除状态和 Note/评论归属校验；修复关闭模型时服务无法启动及 Agent 集成测试未进入默认测试的问题。
2. 已建立评论 Run 的首版状态机、Agent 执行 Outbox、Quorum 执行队列、数据库条件抢占、Lease/Fencing Token、租约续期、10/60/300 秒 TTL 重试队列与 Reconciliation Job；下一步验证模型调用/发布失败后的检查点恢复，补齐 DLQ 重放。
3. 将 Prompt、模型调用、SSE 和状态迁移从 Controller 下沉至运行编排层；验证断开只结束传输，最终运行仍可查询。
4. 将持久化历史转换为有界 Chat Memory，补齐取消、超时、幂等重放、历史分页和留存。
5. 建立课程/章节/PUBLIC Note 索引事件、语义切分、Qdrant upsert/delete 和带来源回答。
6. 接入经过鉴权的个人只读工具，验证越权、Prompt 注入、场景白名单和最小化上下文。
7. 补齐预算、限流、可观测性、固定评估集和网关至模型端到端验收。

进入下一步前至少通过：两个 Agent 实例竞争同一运行时只有一个调用模型；重复事件和执行消息各投递 10 次只产生 1 条回复；已有持久化回答的 `PUBLISH` 阶段失败不会重新生成；旧 Worker 的 Fencing Token 不能覆盖新执行者；公开评论场景无法注册或调用私人工具；删除/下架资料不会再被引用；同会话并发请求得到稳定 409；模型超时、SSE 断开和 Qdrant 不可用均留下可审计运行状态。
