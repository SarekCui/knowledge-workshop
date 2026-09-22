# HTTP 接口基础设施规范

## 1. 设计依据

本规范综合以下来源：

- [RFC 9110：HTTP Semantics](https://www.rfc-editor.org/rfc/rfc9110.html)：方法、状态码和无正文响应语义。
- [Spring Framework Web MVC](https://docs.spring.io/spring-framework/reference/web/webmvc.html)：请求映射、参数校验和全局异常处理。
- [阿里巴巴 Java 开发手册/P3C](https://github.com/alibaba/p3c)：分层、异常日志、输入校验、并发、MySQL 和工程质量约束。
- [Zalando RESTful API Guidelines](https://opensource.zalando.com/restful-api-guidelines/)：API First、Problem JSON、幂等和兼容演进。

采用原则不是照搬单一公司的私有格式，而是优先遵守开放协议，再用成熟工程规范补足实现细节。

## 2. 成功响应

普通 JSON 成功响应显式返回 `Result<T>`：

```json
{
  "code": 200,
  "message": "success",
  "requestId": "web-20260903-001",
  "timestamp": "2026-09-03T14:00:00Z",
  "data": {
    "orderId": "order-001"
  }
}
```

- `code` 是稳定的通用类别码；成功固定为 `200`。
- `message` 只用于人类阅读，前端逻辑不得依赖文本。
- `requestId` 同时写入 `X-Request-Id` 响应头，用于日志关联。
- `timestamp` 使用 UTC `Instant` 和 ISO-8601。
- `data` 保持强类型，不用无约束 `Map<String, Object>`。

Controller 显式声明包装类型，禁止用全局 `ResponseBodyAdvice` 偷偷包装，否则容易导致 OpenAPI、泛型类型、文件和流式响应失真。统一返回类使用 `Result` 命名，避免与 OpenAPI 的 `ApiResponse` 注解冲突。

以下响应不包装：`204 No Content`、文件/图片、流式下载、SSE、Actuator 和 OpenAPI 文档。

## 3. 错误响应

普通 JSON 错误同样使用 `Result<Void>`，HTTP 状态与 `code` 保持一致：

```json
{
  "code": 409,
  "message": "拼团名额已满",
  "requestId": "web-20260903-001",
  "timestamp": "2026-09-03T14:00:00Z",
  "data": null
}
```

字段校验失败由 `message` 汇总安全的字段提示。不得返回堆栈、SQL、表名、中间件地址或原始异常消息。当前只保留少量通用类别码；没有真实客户端分支需求时不新增细粒度业务码。

| HTTP 状态 | 使用场景 |
|---|---|
| 400 | JSON 不可解析、参数格式错误、字段校验失败 |
| 401 | 未登录或令牌无效 |
| 403 | 已认证但无资源权限 |
| 404 | 业务资源不存在 |
| 409 | 状态迁移冲突、版本冲突、幂等键参数冲突、名额已满 |
| 405 | HTTP 方法不支持 |
| 415 | Content-Type 不支持 |
| 429 | 触发限流，必要时携带 `Retry-After` |
| 500 | 未预期系统异常；对外使用通用描述，对内记录异常堆栈 |
| 503 | 依赖暂时不可用且服务无法处理请求 |

## 4. 请求追踪

- 入口读取 `X-Request-Id`；仅接受 1—64 位字母、数字、点、下划线、冒号和连字符。
- 非法或缺失时生成 UUID，防止日志注入和无界字段。
- 请求 ID 写入 MDC、响应头、成功响应和错误响应；跨服务 HTTP 与消息事件继续透传。
- `requestId` 用于技术追踪，不代替业务幂等键。
- 所有可运行服务使用 Spring Boot 内置 Logstash JSON 控制台格式，固定包含 `service`、日志级别、日志类和消息。
- HTTP 完成日志使用 `event=http_request`，并记录 `requestId`、`method`、`path`、`status` 与 `durationMs`；Gateway 额外记录 Reactor 终止信号。
- 禁止在访问日志中记录请求体、响应体、Authorization、Cookie、刷新令牌和 URL 查询参数。

## 5. 幂等与并发

- 创建型 `POST` 优先使用数据库唯一约束保护业务次级键。
- 需要网络失败后原样安全重试时使用业务幂等键，并保存请求摘要和首次结果；相同键不同参数返回 409。当前已有写入 DTO 统一使用
  `idempotencyKey` 字段，格式为 `调用方:操作:稳定标识`；后续再整体迁移到 HTTP `Idempotency-Key` 请求头，禁止同一接口同时接受两种来源。
- 更新资源使用版本号或 `ETag/If-Match`，版本不一致返回 409。
- 拼团参团使用 `groupId + JWT sub` 作为自然业务幂等键，数据库参与记录和交易订单均以 `(group_id, user_id)` 唯一约束兜底；`X-Request-Id` 不参与幂等。

## 6. 查询与分页

- 小型有界集合可直接返回数组；排行榜 `limit` 上限为 1000。
- 普通后台列表使用 `items + page + pageSize + total`，页大小默认 20、最大 100。
- 高频时间线或数据持续变化的列表使用游标分页，返回 `items + nextCursor + hasMore`，不暴露数据库偏移实现。
- 排序字段必须白名单化，禁止把客户端字符串直接拼接到 SQL。

## 7. 跨服务契约

- 外部 HTTP：统一经过 Gateway，路径以 `/api` 开头。
- 内部同步 HTTP：路径以 `/internal` 开头，使用独立鉴权和明确超时；当前尚未启用 OpenFeign。
- 异步事件：RabbitMQ 至少一次投递，事件包含 `eventId`、`eventType`、`occurredAt`、`aggregateId`、`version`、`requestId` 和负载。
- HTTP 错误对象不能直接作为 MQ 失败消息；消息重试、死信和补偿状态使用事件契约表达。

## 8. 兼容性与门禁

- OpenAPI 是外部 HTTP 的机器可读契约，代码、文档和测试同时变更。
- 响应新增可选字段属于兼容变更；删除字段、改变字段类型或语义属于破坏性变更。
- 客户端必须忽略未知响应字段，但服务端严格校验已知输入字段的格式和上限。
- 合并前至少验证正常响应、字段校验、资源不存在、状态冲突和系统异常脱敏。
