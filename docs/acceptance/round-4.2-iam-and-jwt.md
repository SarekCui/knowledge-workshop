# 第 4.2 轮验收记录：IAM 与 JWT 认证

- Git commit：当前目录尚未初始化 Git，暂记为 `N/A`
- 验收日期：2026-09-08
- 环境：JDK 17.0.16、Maven 3.9.9、MySQL 8.4、Nacos 3.0.3

## 完成内容

- 新增 `knowledge-iam`，独占 `knowledge_iam` schema，并按身份领域采用 Controller、Service、DAO、DTO、BO、VO、DO 分层。
- 密码使用 BCrypt 哈希，账号不存在、密码错误和账号不可用均收敛为统一 401，避免暴露账号状态。
- IAM 使用 HS256 签发 30 分钟短期 JWT；密钥仅从 `JWT_SECRET` 注入，并校验至少 32 字节。
- JWT 包含用户 ID、用户名、角色、签发方、签发/过期时间及唯一令牌 ID。
- 网关使用 Spring Security Resource Server 校验签名、签发方和有效期；登录与健康检查匿名开放，其他 `/api/**` 默认要求认证。
- 网关认证失败和拒绝访问均返回统一 `Result<Void>`，保留真实 HTTP 401/403 与请求 ID。
- IAM 登录接口提供 OpenAPI 文档，没有为每个状态码重复堆叠 Controller 注解。

## 验收命令与结果

```bash
mvn -Dmaven.repo.local=.m2/repository test
mvn -Dmaven.repo.local=.m2/repository -Pacceptance verify
```

| 验收项 | 结果 |
|---|---|
| 8 个 Maven 模块编译、打包 | 通过 |
| 27 个单元测试 | 通过，0 失败、0 错误 |
| IAM 3 个 Testcontainers 场景 | 通过，0 失败、0 错误 |
| 营销 5 个、积分 3 个回归场景 | 通过，0 失败、0 错误 |
| 无令牌经网关访问积分接口 | HTTP 401，统一响应与请求 ID 正确 |
| 演示账号经网关登录 | HTTP 200，返回 1800 秒 Bearer JWT |
| 合法 JWT 经网关访问积分接口 | HTTP 200，完成 Nacos 动态路由 |
| IAM `/v3/api-docs` 登录契约 | 通过 |

## 当前安全边界

本轮完成的是“身份认证”最小闭环，不等于完整权限系统：

- 网关负责确认调用者身份，并把原始 Bearer Token 转发给下游。
- 角色声明已经进入 JWT，但尚未定义管理员等接口级角色规则。
- 笔记归属、课程权益、订单归属等资源级权限必须由业务服务最终校验，尚未实现。
- 本地开发服务端口可直接访问；生产部署必须通过网络策略禁止公网绕过网关直连业务服务。
- 刷新令牌轮换、注销撤销、密码找回、MFA、密钥轮换与 JWKS 尚未实现。

## 结论

IAM 登录、JWT 签发和网关统一认证已形成可运行、可测试、可演示的闭环。第 4 轮仍需继续资源级授权和服务容错，不能把本轮结果描述为完整 RBAC 或完整 IAM 平台。
