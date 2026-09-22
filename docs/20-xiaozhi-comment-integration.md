# 小智 Agent 评论区集成开发记录

## 背景

在笔记详情页评论区接入小智 AI 学习助手：用户在评论中 `@小智`，小智异步生成回复并嵌套在该评论下。

## 当前原型链路

```
用户发评论(含@小智)
  → NoteCommentService 正则检测
  → AgentMentionOutboxDO (PENDING)
  → MQ: agent.mentioned.v1 (DLX=knowledge.events.dlx)
  → Agent 执行 Outbox + RabbitMQ Worker
  → OAuth2 Client Credentials 向 IAM 获取并缓存 RS256 服务令牌
  → OpenFeign GET /internal/notes/{noteId}/comments/{commentId}/agent-context (取笔记上下文)
  → 调 DeepSeek
  → OpenFeign POST /internal/notes/{noteId}/comments (写回，parentCommentId=源评论)
```

当前链路只用于原型验证，定时 Worker 的“查询 PENDING 后无条件更新 RUNNING”不能作为多实例生产实现。目标链路以 ADR-0015 为准：

```text
AgentMentioned
  → Agent Inbox 去重
  → 同事务创建 agent_run + agent_execution_outbox
  → Publisher Confirm 投递 AgentRunRequested
  → RabbitMQ Quorum 执行队列（手动 ACK、有限 Prefetch、DLQ）
  → Worker 使用 MySQL 条件更新 + Lease + Fencing Token 抢占
  → status=PENDING/RUNNING/RETRY_WAIT/SUCCEEDED
  → stage=CONTEXT/GENERATE/PUBLISH
  → 持久化回答后幂等发布回复
```

模型结果必须在进入 `PUBLISH` 阶段前持久化。发布失败只重试发布，不能重新调用 DeepSeek；租约过期、执行 Outbox 未投递和未发布回答由 Reconciliation Job 恢复。

## 前端组件结构

### 评论区（Codex 重构版）

- **CollapsibleCommentContent**：长评论折叠，超过 154px 显示"查看更多/收起"
- **InlineReplyBox**：内联回复框，带头像、取消/回复按钮、"问小智"按钮
- **NoteCommentNode**：树状递归渲染
- **评论点赞**：单条评论 toggleCommentLike
- **展开/折叠回复**：根评论默认折叠，点"展开 N 条回复"显示前 10 条，"加载更多"每次加 10 条
- **回复层级**：二级及以下回复直接平铺，不再嵌套折叠

### 小智面板

- 右侧常驻非模态面板，不遮罩主内容
- 宽度可拖拽调整（360~720px），localStorage 持久化
- 思考计时器：思考中实时秒表，结束后显示"已思考 Xs"
- 历史会话列表 + 删除
- 顶部栏与主顶栏对齐（76px）

## UI 精修记录

| 项目 | 方案 |
|---|---|
| 悬浮操作栏 | 底部固定，毛玻璃 backdrop-filter blur(30px)，高度 48px |
| 小智圆形按钮 | 48x48，蓝色渐变 (#5b8def→#7aa8f5)，和悬浮栏同高 |
| "问小智"按钮 | 蓝色渐变胶囊，✦ sparkle 图标，hover 不上浮 |
| 评论 icon | 圆角胶囊聊天气泡 |
| AI 标签 | 浅蓝底 (#e6f4ff) 蓝字 (#1677ff)，9px 小字，14px 高 |
| 代码块 | GPT 风格浅灰底 + 复制按钮 (Copy→Check)，语言标签正确大小写 (Java/Python) |
| 作者元信息 | 头像左垂直居中，右边两行：用户名 + 日期·类型 |
| 字体粗细 | 评论区和作者名统一 500 |
| 回复块 | 背景 #eceef2，和主评论间隔 10px |
| 回复动画 | 出现时 0.25s 淡入下滑 |

## 关键文件

### 前端
- `knowledge-web/src/note/NoteInteractionPanel.tsx` — 评论区主组件
- `knowledge-web/src/note/NoteInteractionStore.ts` — 评论状态管理
- `knowledge-web/src/note/commentThreads.ts` — 树结构
- `knowledge-web/src/agent/AgentDrawer.tsx` — 小智面板

### 后端
- `NoteCommentService.java` — @小智检测 + 软删子回复
- `AgentInternalNoteController.java` — 统一评论资源的 context + 创建接口
- `MentionReplyWorker.java` — RabbitMQ 执行 Worker
- `ServiceAccessTokenService.java` — IAM Client Credentials 获取与到期前缓存刷新

## 已知问题

1. Nacos 服务注册 IP 可能取到网卡地址（手机热点），导致网关 503。重启时需加 `--spring.cloud.nacos.discovery.ip=127.0.0.1`
2. 当前 Agent Worker 没有原子抢占、Lease/Fencing Token、阶段检查点和业务重试，不能安全多实例部署；目标方案见 ADR-0015
3. 评论 @小智 后无流式输出，等 DeepSeek 全量返回
4. 当前上下文接口需补齐 PUBLIC、删除状态及 Note/评论归属校验，并从 Controller 直接访问 Mapper 改为 Service 编排
5. 当前集成测试在模型关闭时因条件装配失败而无法启动，且普通 `mvn test` 不执行 `*IT`，需纳入 acceptance/CI 验收
