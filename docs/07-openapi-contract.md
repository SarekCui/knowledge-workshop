# OpenAPI 接口契约

## 1. 目标

在 React 前端开发前提供稳定、机器可读的 HTTP 契约。OpenAPI 描述接口路径、方法、请求字段、响应结构、错误状态和示例；业务状态机与一致性语义仍以 `docs/03-domain-and-consistency.md` 为准。

## 2. 文档地址

| 服务 | OpenAPI JSON | Swagger UI |
|---|---|---|
| marketing | `http://localhost:8081/v3/api-docs` | `http://localhost:8081/swagger-ui.html` |
| points | `http://localhost:8082/v3/api-docs` | `http://localhost:8082/swagger-ui.html` |
| iam | `http://localhost:8083/v3/api-docs` | `http://localhost:8083/swagger-ui.html` |
| learning | `http://localhost:8084/v3/api-docs` | `http://localhost:8084/swagger-ui.html` |

## 3. 当前外部接口

| 服务 | 方法 | 路径 | 幂等键/查询边界 |
|---|---|---|---|
| marketing | GET | `/api/marketing/activities`、`/{activityId}`、`/{activityId}/groups`、`/groups/{groupId}` | 仅查询当前可参与的活动及有效团实例，分页上限 100 |
| marketing | POST | `/api/marketing/groups/{groupId}/join` | JWT `sub` + `groupId`；无请求体 |
| marketing | GET | `/api/marketing/orders`、`/{orderId}` | 仅返回 JWT 当前用户的订单，支持状态筛选与分页 |
| marketing | POST | `/api/marketing/orders/{orderId}/pay` | `paymentTradeNo` |
| marketing | GET/POST/PUT/PATCH | `/api/marketing/admin/activities/**` | 仅 `ADMIN`；创建以调用方提供的资源 ID 幂等，修改使用 `version` |
| marketing | GET/POST | `/api/marketing/admin/notification-tasks/**` | 仅 `ADMIN`；查询异常任务，人工重试只把 `RETRY/DEAD` 任务重新排入调度 |
| points | POST | `/api/points/sign-ins` | JWT `sub` + UTC 日期；请求体不接收用户 ID |
| points | GET | `/api/points/leaderboard` | `limit` 限制为 1—1000 |
| learning | GET | `/api/learning/courses`、`/{courseId}`、`/{courseId}/chapters` | 只返回已发布内容 |
| learning | POST/GET/PUT/PATCH/DELETE | `/api/learning/notes/**` | JWT `sub` 为所有者；创建以 `clientRequestId` 幂等，修改使用 `version` |
| learning | POST/GET | `/api/learning/videos/{videoId}/sessions`、`/progress` | `eventId` 幂等，`sessionEpoch + sequence` 防乱序 |
| learning | GET | `/api/learning/progress/recent`、`/api/learning/entitlements` | 只读取当前 JWT 用户数据 |
| learning | POST/PUT/PATCH/DELETE | `/api/learning/admin/**` | 仅 `ADMIN` 角色；修改使用 `version` |

所有接口接受可选请求头 `X-Request-Id`。缺失、格式非法或超过 64 字符时，服务端生成新值；响应头和响应体均返回最终请求 ID。该值只用于技术追踪，不参与业务幂等。

## 4. 响应和错误

成功与失败统一使用：

```json
{
  "code": "OK",
  "message": "success",
  "requestId": "request-20260903-001",
  "timestamp": "2026-09-03T12:00:00Z",
  "data": {}
}
```

- HTTP 200：成功或幂等返回。
- HTTP 400：参数校验失败。
- HTTP 401/403：未认证或没有资源权限。
- HTTP 404：资源不存在。
- HTTP 409：幂等键、状态或乐观锁版本冲突。
- HTTP 503：可靠消息暂时无法确认。
- HTTP 500：未预期异常，对外只返回稳定错误码，不暴露堆栈。

## 5. 验证

普通单元测试：

```bash
mvn -Dmaven.repo.local=.m2/repository test
```

包含 OpenAPI 契约的真实依赖验收：

```bash
mvn -Dmaven.repo.local=.m2/repository verify -Pacceptance
```

契约测试会验证文档端点、服务标题、关键路径、请求 Schema、`X-Request-Id` 和统一错误语义。

## 6. 前端生成约束

- React 前端从服务的 OpenAPI JSON 生成 TypeScript 类型和请求客户端。
- 生成代码进入 `frontend/packages/api-client`，不手工修改。
- OpenAPI 发生变化时重新生成并通过前端类型检查。
- 多服务文档聚合、Orval 配置和 CI 漂移检查在前端工程基线中实现。
