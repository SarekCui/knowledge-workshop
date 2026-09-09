# 第 3.5 轮 OpenAPI 契约验收记录

## 验收环境

- Java：17.0.16
- Spring Boot：3.5.9
- Springdoc OpenAPI：2.8.15
- 隔离依赖：Testcontainers 1.21.4、MySQL 8.4、Redis 7.4、RabbitMQ 4.1

## 执行命令

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home \
PATH=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home/bin:$PATH \
mvn -Dmaven.repo.local=.m2/repository verify -Pacceptance
```

## 验收结果

执行时间：2026-09-03。

| 场景 | 期望 | 自动化证据 | 结果 |
|---|---|---|---|
| 营销契约端点 | `/v3/api-docs` 返回营销服务标题及全部当前营销路径 | `MarketingAcceptanceIT.openApiContractDocumentsMarketingOperationsAndErrors` | 通过 |
| 营销错误契约 | 操作声明 200、400、500 响应，并声明 `X-Request-Id` | 同上 | 通过 |
| 积分契约端点 | `/v3/api-docs` 返回积分服务标题、签到和排行榜路径 | `PointsAcceptanceIT.openApiContractDocumentsPointsOperationsAndSchemas` | 通过 |
| 积分数据模型 | 签到请求、签到响应和排行榜条目 Schema 可解析 | 同上 | 通过 |
| 全量回归 | 全部模块构建，12 个单元测试和 7 个 Testcontainers 场景通过 | Maven Reactor `verify -Pacceptance` | 通过 |

## 结论

第 3.5 轮退出标准已满足。营销与积分服务提供机器可读 OpenAPI JSON 和 Swagger UI，接口路径、请求模型、响应状态及请求追踪头均由自动化契约测试保护。
