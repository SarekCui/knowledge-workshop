# 小智 AI Agent 初步设计

- 状态：讨论稿
- 日期：2026-09-17
- 对应轮次：5.8（规划中）
- 相关决策：`docs/adr/0014-xiaozhi-agent-service.md`

## 1. 目标

在知识工坊现有课程、学习进度、Note 与评论能力之上增加“小智”AI 学习助手，首期包含两个入口：

1. 全局右侧“问小智”：根据当前页面、课程、章节、学习进度与用户有权访问的 Note 提供流式问答。
2. 公开 Note 评论区 `@小智`：用户明确提及时，基于公开 Note 和当前评论线程异步回复。

首期重点验证上下文感知问答、带来源回答、受控工具调用、异步可靠执行与权限隔离，不以“自主执行任意业务操作”为目标。

## 2. 范围边界

### 2.1 首期包含

- Java 17 + Spring Boot 独立服务 `knowledge-agent`。
- LangChain4j 作为模型、流式输出、工具调用与 RAG 编排框架。
- 右侧抽屉的 SSE 流式问答。
- 评论区 `@小智` 的 Outbox + RabbitMQ 异步回复。
- 已发布课程、章节和 PUBLIC Note 的检索与引用。
- 当前用户自己的课程进度和 Note 只读工具。
- 会话、消息、运行、工具调用、引用、模型与 Token 用量的持久化审计。
- 请求限流、超时、幂等、内容安全和资源级权限校验。

### 2.2 首期不包含

- 多 Agent 协作与自主任务规划。
- 自动支付、参团、发放权益、修改积分或订单状态。
- 模型直接访问业务数据库、Mapper、任意 URL 或执行 SQL。
- 私人 Note 进入公共向量索引。
- 模型训练、微调、OCR、音视频转写和复杂文档解析。
- 未经用户确认直接覆盖或发布 Note。

## 3. 服务边界与数据所有权

`knowledge-agent` 拥有：

- Agent 会话与消息；
- Agent 运行状态、错误与重试信息；
- 工具调用摘要与审计；
- 回答引用及其来源版本；
- Prompt 版本、模型标识和 Token 用量；
- Agent 消费幂等记录。

它不拥有课程、Note、评论、进度、订单、积分、权益和用户资料。这些数据继续由原服务管理，Agent 只能通过受鉴权的内部 API 或事件访问，不直接访问其他服务数据库。

```mermaid
flowchart LR
    Web[React 学习端] --> Gateway[knowledge-gateway]
    Gateway -->|SSE 问答| Agent[knowledge-agent]
    Gateway -->|发布评论| Learning[knowledge-learning]

    Learning --> LearningDB[(learning_db)]
    LearningDB -->|AgentMentioned Outbox| MQ[(RabbitMQ)]
    MQ -->|至少一次投递| Agent

    Agent -->|内部 API| Learning
    Agent -->|后续只读诊断工具| Marketing[knowledge-marketing]
    Agent -->|后续只读查询工具| Points[knowledge-points]
    Agent --> Model[模型供应商]
    Agent --> Vector[(向量存储，选型待定)]
    Agent --> AgentDB[(agent_db)]
    Agent --> Redis[(Redis)]

    Agent -->|幂等发布 AI 回复| Learning
```

## 4. 服务内部结构

服务继续采用业务域优先、域内传统分层：

```text
com.knowledge.agent
├── conversation  # 私人会话、消息和 SSE
├── mention       # @小智事件消费与公开回复
├── retrieval     # 检索、权限过滤和引用组装
├── tool          # 有场景白名单的业务工具
├── model         # LangChain4j 适配和模型端口
├── prompt        # Prompt 模板及版本
├── safety        # 输入、输出和工具调用规则
└── config
```

业务层通过本项目定义的端口调用模型，避免 LangChain4j 类型扩散到 Controller、DAO 与业务契约：

```java
public interface AgentModelService {
    Flux<AgentChunkBO> stream(AgentRequestBO request);
    AgentAnswerBO generate(AgentRequestBO request);
}
```

LangChain4j 实现位于 `model` 域内。是否采用 `AI Services` 接口、低层模型 API 或两者组合，在原型验证后确定。

## 5. 右侧“问小智”

### 5.1 请求流程

```mermaid
sequenceDiagram
    actor U as 用户
    participant W as Web
    participant G as Gateway
    participant A as Agent
    participant L as Learning
    participant R as Retriever
    participant M as Model
    participant D as agent_db

    U->>W: 提交问题
    W->>G: POST messages/stream
    G->>A: JWT + 页面上下文
    A->>A: 鉴权、限流、会话互斥
    A->>L: 查询有权访问的业务上下文
    A->>R: 检索课程与 Note
    A->>M: Prompt + 工具 + 检索片段
    M-->>A: 流式响应
    A-->>W: SSE 事件
    A->>D: 保存结果、引用和用量
```

### 5.2 初步外部契约

```text
POST /api/agent/conversations
GET  /api/agent/conversations?page=1&pageSize=20
GET  /api/agent/conversations/{conversationId}/messages
POST /api/agent/conversations/{conversationId}/messages/stream
POST /api/agent/runs/{runId}/cancel
```

消息请求包含客户端 UUID 幂等键、问题和页面上下文。课程、章节、视频、Note 等 ID 仅作为查询提示，服务端必须使用 JWT 身份重新校验可见性。

SSE 事件初步定义：

```text
run.started
answer.delta
citation.added
tool.started
tool.completed
answer.completed
answer.failed
```

不向用户输出模型内部思维过程，只展示回答、必要的工具执行状态与可验证引用。

## 6. 评论区 `@小智`

### 6.1 可靠执行流程

```mermaid
sequenceDiagram
    actor U as 用户
    participant L as Learning
    participant DB as learning_db
    participant Q as RabbitMQ
    participant A as Agent
    participant M as Model

    U->>L: 发布包含 @小智 的评论
    L->>DB: 同事务写评论与 Outbox
    L-->>U: 评论发布成功
    L->>Q: AgentMentioned
    Q->>A: 至少一次投递
    A->>A: eventId 幂等
    A->>L: 获取公开 Note 与评论上下文
    A->>M: 生成公开回答
    A->>L: 使用幂等键发布 AI 回复
    L->>DB: 保存标记为 AI 的评论
```

事件仅携带稳定 ID，不携带完整 Note 正文，避免消息过大、过期快照及私人内容泄漏。建议事件字段为：

```text
eventId, eventType, occurredAt, aggregateId, version,
noteId, commentId, parentCommentId, requesterId
```

AI 回复使用 `AGENT_COMMENT_REPLY:{sourceCommentId}` 作为业务幂等键，并由数据库唯一索引兜底。失败进入有限重试，超过上限进入死信和可观测失败状态；不能影响用户原评论提交。

## 7. 工具与权限模型

首期按场景建立不同工具白名单。

私人问答可用：

- 查询当前用户课程与学习进度；
- 查询当前用户自己的 Note；
- 查询公开课程和 PUBLIC Note；
- 在用户确认后创建 Note 草稿。

公开评论可用：

- 查询当前 PUBLIC Note；
- 查询当前公开评论线程；
- 查询公开课程目录和公开知识库。

公开评论 Agent 不注册私人进度、私人 Note、订单、积分等工具。`userId`、角色和权限范围必须由 JWT 与服务端执行上下文注入，不能接受模型参数作为可信身份。

每次工具执行都由目标服务重新做资源级权限校验。工具只能调用目标服务 Service 暴露的内部接口，不得绕过目标服务访问 Mapper 或数据库。

## 8. RAG 初步设计

公共索引首期候选数据：

- 已发布课程简介；
- 已发布章节文本；
- 已授权使用的课程字幕；
- PUBLIC Note；
- 平台帮助文档。

公共索引不包含 PRIVATE/DRAFT Note、订单、用户资料和私人学习数据。个人 Note 通过实时权限查询注入当前请求上下文，不进入公共索引。

向量文档至少保存以下元数据：

```text
documentId, sourceType, sourceId, sourceVersion,
courseId, chapterId, visibility, ownerId,
contentHash, updatedAt
```

检索后仍需执行可见性校验。向量数据库、Embedding 模型、切分策略、混合检索与重排方案均为待验证项，不在本文提前定案。

## 9. 数据模型草案

`agent_db` 首批候选表：

| 表 | 用途 | 关键约束 |
|---|---|---|
| `agent_conversation` | 会话与所属用户 | 用户只能访问自己的会话 |
| `agent_message` | 用户/助手消息 | `(conversation_id, client_request_id)` 唯一 |
| `agent_run` | 单次同步或异步运行 | 评论场景按来源评论唯一 |
| `agent_tool_call` | 工具、耗时与结果摘要 | 不保存令牌和非必要敏感参数 |
| `agent_citation` | 回答引用与来源版本 | 可追溯到业务资源 |
| `consumed_event` | MQ 消费幂等 | `(event_id, consumer)` 唯一 |

完整消息与运行记录以 MySQL 为事实源；LangChain4j Chat Memory 只负责挑选本轮模型上下文，不作为聊天历史事实源。

## 10. 并发、超时与失败恢复

- 同一 `conversationId` 同时只允许一个生成任务，避免 LangChain4j Chat Memory 并发损坏和回答交错。
- 会话锁 Key 初步采用 `kw:agent:conversation:lock:{conversationId}`；Redis 不是运行状态事实源。
- 同步问答超时后不透明重新生成，避免重复计费和非确定结果。
- 评论 Agent 依赖消息重试、消费幂等和发布评论幂等恢复。
- 模型调用成功但持久化失败时，运行必须进入可识别的失败状态，不得把未审计回答标记为完成。
- SSE 断开不等同于模型任务必然取消；取消、继续执行及计费语义后续单独确认。

初始预算仅作为原型起点，需经测试调整：

| 环节 | 初始预算 |
|---|---:|
| 普通内部查询工具 | 1 秒 |
| RAG 检索 | 2 秒 |
| 首 Token | 10 秒 |
| 单次模型总运行 | 30 秒 |
| 单轮工具调用 | 最多 5 次 |

## 11. 安全与治理

- 将系统指令、用户输入、检索资料和工具结果作为不同信任层处理。
- Note、评论和课程正文均视为不可信数据，其中的指令不得提升权限或改变工具白名单。
- 输入、输出与工具参数设置长度和数量上限。
- 写工具首期仅允许创建草稿，并要求用户确认；不开放资金、权益、积分和订单写操作。
- AI 评论显示“小智 · AI 学习助手”和“AI 生成”标识，并提供反馈或举报入口。
- 日志不记录完整 Prompt、令牌、模型密钥或不必要的个人数据。
- 保存 Prompt 版本、模型标识、Token 用量、工具摘要和引用，满足问题复现与成本分析。

## 12. 可观测性

至少监控：

- 活跃 SSE 连接数；
- 首 Token、完整回答和工具调用耗时；
- 模型错误率、超时率和取消率；
- 输入/输出 Token 与估算成本；
- RAG 召回耗时和无结果率；
- 工具调用成功率与权限拒绝数；
- 评论 Agent 队列积压、重试和死信数量；
- 各场景限流与会话冲突次数。

## 13. 分阶段落地

1. 建立 ADR、契约和威胁模型，确定模型与向量存储选型。
2. 创建 `knowledge-agent`、独立 schema、运行审计和模型适配层。
3. 实现无工具的右侧流式问答，验证 SSE、超时、取消和持久化。
4. 接入公开课程与 PUBLIC Note 的带引用检索。
5. 接入经过鉴权的个人进度与个人 Note 只读工具。
6. 实现评论 `@小智` Outbox、MQ、幂等消费和 AI 回复。
7. 增加内容安全、限流、成本控制、指标和故障演练。
8. 经用户确认后开放创建 Note 草稿，其他写工具继续后置。

## 14. 待确认问题

- 模型供应商、模型名称、兼容协议与预算上限。
- Embedding 模型及向量存储选型，是否先使用已有 MySQL 能力完成关键词检索原型。
- 课程字幕或正文的来源、版权和更新机制。
- SSE 断开后的取消与结果保留语义。
- 对话和审计数据保留周期及用户删除策略。
- 评论区频率限制、单线程最大追问次数和失败展示方式。
- 引用粒度、答案质量评估集和上线阈值。
- `@小智` 解析规则、同名用户冲突与历史评论兼容方式。

本文是方向性初稿，不代表能力已经实现或验收。上述待确认项在编码前继续收敛，并在对应 ADR 或后续设计文档中记录最终决策。
