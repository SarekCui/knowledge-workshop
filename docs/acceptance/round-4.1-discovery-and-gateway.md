# 第 4.1 轮验收记录：注册发现与网关路由

- Git commit：当前目录尚未初始化 Git，暂记为 `N/A`
- 验收日期：2026-09-08
- 环境：JDK 17.0.16、Maven 3.9.9、Nacos 3.0.3、Docker 28.3.0

## 完成内容

- 营销、积分和网关服务通过 Nacos 注册发现，不再依赖网关中的固定实例地址。
- 网关使用 `lb://knowledge-marketing` 与 `lb://knowledge-points` 路由，并使用 Spring Cloud LoadBalancer 的 Caffeine 生产级缓存实现。
- 网关统一校验或生成 `X-Request-Id`，覆盖下游重复响应头，确保响应头只有一个请求 ID 且与统一响应体一致。
- Nacos 地址、命名空间、分组及网关目标 URI 均支持环境变量覆盖。
- 本地 Compose 提供 Nacos 3 单机开发环境；生产环境仍要求集群与认证。

## 验收命令与结果

```bash
mvn -Dmaven.repo.local=.m2/repository test
mvn -Dmaven.repo.local=.m2/repository -Pacceptance verify
```

| 验收项 | 结果 |
|---|---|
| 7 个 Maven 模块编译、打包 | 通过 |
| 21 个单元测试 | 通过，0 失败、0 错误 |
| 营销服务 5 个 Testcontainers 集成场景 | 通过，0 失败、0 错误 |
| 积分服务 3 个 Testcontainers 集成场景 | 通过，0 失败、0 错误 |
| 三个应用注册到 Nacos | 通过 |
| 网关经 `lb://` 访问积分服务 | 通过，HTTP 200 |
| 合法请求 ID 透传 | 通过 |
| 非法请求 ID 替换且响应头去重 | 通过，响应头与响应体一致 |

## 已知非阻塞告警

- macOS 本地运行时 Netty 会提示未加载原生 DNS 解析器并回退到系统实现，不影响本轮本地联调；容器化 Linux 部署不涉及该 macOS 告警。
- 当前 Flyway 版本提示其已测试的 MySQL 上限低于验收使用的 MySQL 8.4；迁移实际成功，后续依赖升级时继续核对兼容矩阵。
- Nacos 3 的旧版 `/nacos/v1/console/health/readiness` 接口已不可作为当前控制台健康检查依据，本轮以启动日志、注册成功日志和真实路由请求共同验收。

## 尚未完成

- 多实例轮询分布验证与实例下线摘除测试。
- JWT 网关认证、服务内资源鉴权、超时、熔断、隔离与有限重试。
- 结构化日志、指标与跨服务链路追踪。

## 结论

第 4 轮的注册发现与动态路由基础已完成，可以继续开发认证授权与服务容错；第 4 轮整体尚未结束。
