# ADR-0006：统一 HTTP 响应模型

- 状态：已接受
- 日期：2026-09-07

## 背景

ADR-0005 将成功响应设计为 `ApiResult<T>`、错误响应设计为 RFC 9457 `ProblemDetail`。项目尚未发布外部客户端，维护者希望参考同工作区成熟项目，降低前后端接入和异常模型的理解成本，同时减少业务错误码数量。

## 决策

- 普通 JSON 成功与错误响应统一显式使用 `Result<T>`，字段为 `code`、`message`、`requestId`、`timestamp` 和 `data`。
- `code` 使用少量通用 HTTP 类别码，如 400、404、409、500；具体业务信息放在稳定且安全的 `message` 中。
- HTTP 响应状态必须表达真实结果，不使用 HTTP 200 包装失败。
- Controller 显式声明 `Result<VO>`，不使用全局 `ResponseBodyAdvice` 隐式包装。
- `204`、文件、图片、流式下载、SSE、Actuator 和 OpenAPI 文档不包装。

## 备选方案

- 保留 RFC 9457：协议标准化程度更高，但当前项目会同时维护两套响应结构，增加 React 客户端生成和教学演示成本。
- 成功与错误均返回 HTTP 200：实现简单，但会破坏网关、监控、缓存和客户端的 HTTP 语义，因此拒绝。

## 影响

### 正面影响

- 前后端只维护一种 JSON 外壳。
- 不再与 Swagger `ApiResponse` 注解重名，也不需要全限定类名。
- 通用错误码数量较少，符合当前项目规模。

### 代价与风险

- 不再直接兼容 RFC 9457 客户端。
- 前端仍必须先判断 HTTP 状态，不能只判断响应体 `code`。
- `message` 不应用于需要长期稳定的复杂业务分支；未来出现真实需求时再增加少量业务子码。

## 验证方式

- common 异常处理测试验证 HTTP 状态、`code`、`message` 和 `requestId`。
- marketing、points 的 OpenAPI 与集成测试验证 Controller 契约。
