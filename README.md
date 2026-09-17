# 知识工坊（Knowledge Workshop）

知识工坊是面向课程学习、技术社区、知识付费与营销活动的微服务练习项目。项目采用“先建立业务闭环，再逐步增加基础设施复杂度”的建设方式，避免先堆叠大量空服务。

## 当前实现

项目采用传统分层架构与 `com.knowledge` 根包名（不包含 `cyf`），并已完成首批简历能力的可编译实现：

- `knowledge-components`：可复用技术组件聚合模块，包含对象存储与分布式锁组件。
- `knowledge-lock-spring-boot-starter`：AOP + Redisson 注解锁，支持可重入、公平、读写锁和有序多重锁。
- `knowledge-object-storage`：S3 兼容对象存储抽象及 MinIO 实现。
- `knowledge-security`：项目内部安全模块，为 Servlet 服务提供 JWT 验签、角色映射及 401/403 响应。
- `knowledge-iam`：用户身份、BCrypt 密码校验、短期 JWT、刷新令牌轮换/复用检测、注销撤销及 OpenAPI 契约。
- `knowledge-web`：React学习端，课程广场/课程详情/我的学习/播放空间独立路由、HttpOnly刷新Cookie恢复登录、注销，以及10秒心跳与暂停/拖动后续播保存。
- `knowledge-marketing`：责任链校验、Redis Lua 原子占位/回补、本地事务订单与成团、可靠通知任务和定时补偿，并提供用户查询与管理员运营接口。
- `knowledge-points`：Bitmap 签到、RabbitMQ 异步积分、季度物理分表、赛季账户与 ZSet 排名、赛季运营与幂等结算、历史快照分页、XXL-JOB 快照/归档/结算处理器。
- `knowledge-learning`：课程分类与检索、安全章节目录、技术 Note（Markdown、标签、私有图片、分享互动）、课程权益、我的可学习课程分页及课程级完成率，以及支持多设备乱序保护和 Redis 恢复的视频学习进度。
- `knowledge-gateway`：基于 Nacos 服务发现和 Spring Cloud LoadBalancer 的动态路由与 JWT 校验，并提供超时、独立熔断、并发隔离、GET 有限重试和统一降级。

目前 53 个单元测试和 38 个集成验收场景通过。验收覆盖 BCrypt 登录与 JWT 签发、刷新令牌哈希存储、轮换、复用检测和幂等注销、网关与业务服务双层验签、JWT 用户身份落库、订单所有权、拼团查询与运营权限、IAM/营销/积分/学习 OpenAPI 契约、100 个并发请求竞争 10 个拼团名额、事务失败名额回补、通知失败重试与人工重试、成团权益幂等发放、Note 所有权与乐观锁、视频进度重复/乱序/多设备处理、原子并发播放会话、MQ不可路由与确认超时、确认后缓存失败、Redis 进度恢复、我的课程分页与权益去重、当前视频版本课程完成率、RabbitMQ 死信留存、Redis 榜单重建、赛季配置、并发结算、未入账任务阻止结算、结算失败回滚与重试、晚到事件留存、历史榜单分页，以及网关超时、熔断、并发隔离、读请求有限重试和写请求不重试。该结果是正确性验收，不是持续性能压测，因此 README 不宣称已达到某个 RPS 或生产容量。

IAM、营销、积分和网关统一输出 Logstash JSON 日志；HTTP 完成日志包含服务名、请求 ID、方法、路径、状态码和耗时，不记录请求体、查询参数与认证凭证。

## 文档索引

- [产品范围与业务边界](docs/00-product-scope.md)
- [系统架构设计](docs/01-architecture.md)
- [工程设计约束](docs/02-engineering-constraints.md)
- [核心领域与一致性设计](docs/03-domain-and-consistency.md)
- [多轮次建设目标](docs/04-iteration-roadmap.md)
- [质量与验收标准](docs/05-quality-and-acceptance.md)
- [视频学习进度企业级设计](docs/06-video-progress-design.md)
- [OpenAPI 接口契约](docs/07-openapi-contract.md)
- [HTTP 接口基础设施规范](docs/08-api-infrastructure.md)
- [分布式锁 Starter 设计](docs/09-distributed-lock-starter.md)
- [第 2 轮拼团验收记录](docs/acceptance/round-2-marketing.md)
- [第 3 轮积分验收记录](docs/acceptance/round-3-points.md)
- [第 3.5 轮 OpenAPI 验收记录](docs/acceptance/round-3.5-openapi.md)
- [第 3.6 轮接口基础设施与分包验收记录](docs/acceptance/round-3.6-http-and-layering.md)
- [第 4.1 轮注册发现与网关路由验收记录](docs/acceptance/round-4.1-discovery-and-gateway.md)
- [第 4.2 轮 IAM 与 JWT 认证验收记录](docs/acceptance/round-4.2-iam-and-jwt.md)
- [第 4.3 轮服务内认证与资源授权验收记录](docs/acceptance/round-4.3-service-authorization.md)
- [第 4.4 轮刷新令牌轮换与注销验收记录](docs/acceptance/round-4.4-refresh-token.md)
- [第 4.5 轮网关同步调用容错验收记录](docs/acceptance/round-4.5-gateway-resilience.md)
- [第 4.6 轮结构化日志验收记录](docs/acceptance/round-4.6-structured-logging.md)
- [第 5.1 轮学习主线验收记录](docs/acceptance/round-5.1-learning-core.md)
- [第 5.2 轮营销查询与运营验收记录](docs/acceptance/round-5.2-marketing-operations.md)
- [第 5.3 轮赛季配置验收记录](docs/acceptance/round-5.3-season-configuration.md)
- [第 5.3 轮赛季结算与历史榜单验收记录](docs/acceptance/round-5.3-season-settlement.md)
- [第 5.4 轮课程进度与我的课程验收记录](docs/acceptance/round-5.4-course-learning.md)
- [第 5.5 轮断点续播加固验收记录](docs/acceptance/round-5.5-resume-hardening.md)
- [技术 Note 图片设计](docs/17-technical-note-images.md)
- [第 5.6 轮技术 Note 图片验收记录](docs/acceptance/round-5.6-technical-note-images.md)
- [课程目录与学习入口设计](docs/18-course-catalog-design.md)
- [第 5.7 轮课程目录验收记录](docs/acceptance/round-5.7-course-catalog.md)
- [架构决策记录](docs/adr/README.md)

## 技术栈

- Java 17、Maven
- Spring Boot 3.5.x、Spring Cloud 2025.0.x
- Spring Cloud Gateway、Spring Security、JWT、Nacos、Resilience4j
- MyBatis-Plus、MySQL
- Redis、Redisson、RabbitMQ
- XXL-JOB
- JUnit 5、Testcontainers、ArchUnit

锁定版本见根目录 `pom.xml`，当前包括 Spring Boot 3.5.9、Spring Cloud 2025.0.0、MyBatis-Plus 3.5.17、Redisson 3.50.0、XXL-JOB 3.4.2 与 Springdoc OpenAPI 2.8.15。

## 本地运行

要求 JDK 17、Maven 3.9+ 与 Docker Compose。Compose 会启动开发用单机 Nacos；该无鉴权配置不得用于生产。

```bash
docker compose up -d
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home
export JWT_SECRET='replace-with-at-least-32-random-bytes'
mvn test
mvn package -DskipTests
java -jar knowledge-iam/target/knowledge-iam-0.1.0-SNAPSHOT.jar
java -jar knowledge-marketing/target/knowledge-marketing-0.1.0-SNAPSHOT.jar
java -jar knowledge-points/target/knowledge-points-0.1.0-SNAPSHOT.jar
java -jar knowledge-learning/target/knowledge-learning-0.1.0-SNAPSHOT.jar
java -jar knowledge-gateway/target/knowledge-gateway-0.1.0-SNAPSHOT.jar
```

也可以在完成打包后使用统一脚本启动或停止全部业务服务与前端。脚本只在进程内生成本次启动共用的临时JWT密钥，不输出或写入密钥：

```bash
./scripts/start-local.sh
./scripts/stop-local.sh
```

启动脚本以前台监督方式运行，请保持该终端打开，按`Ctrl+C`安全停止全部服务；也可在另一个终端执行停止脚本。日志与PID文件位于`/private/tmp/knowledge-workshop`，前端地址为`http://127.0.0.1:5173/`。

Flyway 首次启动后初始化演示活动：

```bash
docker compose exec -T mysql mysql -uroot -proot < scripts/demo-data.sql
```

RabbitMQ 管理页为 `http://localhost:15672`，账号密码均为 `knowledge`。XXL-JOB 默认关闭；接入 Admin 后设置 `XXL_JOB_ENABLED=true`。
Nacos 客户端端口为 `8848`，控制台映射到 `http://localhost:8849/`。Nacos 3 已将服务 API 与控制台拆分到不同端口。
Compose 将 MySQL/Redis 分别暴露在宿主机 `3307`/`6380`，避免与常见的本机 `3306`/`6379` 冲突。

示例请求：

```bash
curl -X POST http://localhost:8080/api/iam/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"demo","password":"Knowledge@123"}'

# 将登录响应 data.accessToken 的值放入 ACCESS_TOKEN，再访问受保护接口。
export ACCESS_TOKEN='<data.accessToken>'

curl -X POST http://localhost:8080/api/marketing/groups/group-demo/join \
  -H "Authorization: Bearer ${ACCESS_TOKEN}"

curl -X POST http://localhost:8080/api/points/sign-ins \
  -H "Authorization: Bearer ${ACCESS_TOKEN}"
```

`demo / Knowledge@123` 仅是 `scripts/demo-data.sql` 创建的本地演示账号，不得用于其他环境。

若历史 `activity-demo` 显示课程不存在或无可加入团，使用[本地拼团数据修复说明](docs/13-demo-purchase-data-repair.md)补齐缺失课程并通过管理接口开新团，不重置历史订单或成团状态。

## 建设原则

1. 每轮只引入完成该轮业务目标所需的服务和中间件。
2. 每个功能必须同时具备正常路径、异常路径、测试和可观测性。
3. 跨服务默认采用最终一致性，禁止伪造分布式事务语义。
4. 简历、README 和接口文档只描述已经通过验收的能力。
5. 所有重要设计取舍通过 ADR 留痕。
