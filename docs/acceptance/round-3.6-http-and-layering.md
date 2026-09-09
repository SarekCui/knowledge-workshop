# 第 3.6 轮验收记录：接口基础设施与领域分包

- Git commit：当前目录尚未初始化 Git，暂记为 `N/A`
- 验收日期：2026-09-07
- 环境：JDK 17.0.16、Maven 3.9.9、Docker 28.3.0

## 完成内容

- 所有 JSON 接口统一使用 `Result<T>`，成功与异常不再混用 `ApiResult` 和 `ProblemDetail`。
- 保留正确的 HTTP 状态码，并将业务错误收敛为少量通用错误类型。
- Controller 使用 DTO 接收入参、VO 返回展示数据；DO 只用于持久化，BO 用于服务间业务数据传递。
- 营销与积分模块按业务领域分包；每个领域的数据访问层统一使用 `dao.model` 存放 DO、`dao.mapper` 存放 Mapper，跨领域访问通过 Service 边界完成。
- OpenAPI 注解使用普通导入，契约包含请求 ID、DTO/VO Schema 与 200/400/500 响应。
- 积分消息使用季度流水唯一键及 `INSERT IGNORE` 的插入行数原子判重，重复投递作为正常幂等分支处理；表中业务唯一键仅为事件 ID，主键使用服务端 UUID。

## 验收命令与结果

```bash
mvn -Dmaven.repo.local=.m2/repository test
mvn -Dmaven.repo.local=.m2/repository -Pacceptance verify
```

| 验收项 | 结果 |
|---|---|
| 7 个 Maven 模块编译、打包 | 通过 |
| 19 个单元测试 | 通过，0 失败、0 错误 |
| 营销服务 5 个 Testcontainers 集成场景 | 通过，0 失败、0 错误 |
| 积分服务 3 个 Testcontainers 集成场景 | 通过，0 失败、0 错误 |
| MySQL、Redis、RabbitMQ 真实依赖联调 | 通过 |

## 已知非阻塞告警

- 当前 Flyway 版本提示其已测试的 MySQL 上限低于验收使用的 MySQL 8.4；迁移实际成功，后续依赖升级时继续核对兼容矩阵。
- Springdoc 在验收环境默认开放 Swagger 端点；生产环境应通过配置关闭或限制访问。

## 结论

第 3.6 轮退出标准已满足，可以进入第 4 轮服务治理，或先按产品优先级进入第 5 轮课程学习最小闭环。
