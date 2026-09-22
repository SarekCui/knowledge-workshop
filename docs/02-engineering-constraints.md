# 工程设计约束

## 1. 技术与版本

- 基线使用 Java 17，禁止依赖更高版本 JDK 才能编译的语法或 API。
- 依赖版本由根 POM 的 BOM 统一管理，子模块不得随意覆盖。
- Spring Boot、Spring Cloud 与 Spring Cloud Alibaba 必须符合官方兼容矩阵。
- 不使用动态版本、版本范围或 SNAPSHOT 作为稳定分支依赖。
- 每次升级框架版本单独提交，并保留全量构建和集成测试结果。

## 2. 模块依赖

- Java 基础包统一为 `com.knowledge`；服务包格式为 `com.knowledge.{service}`，禁止使用个人姓名或 `cyf` 作为包路径。
- 服务可依赖 `knowledge-api`、`knowledge-common`、`knowledge-security` 和
  `knowledge-components` 下职责单一的技术组件，禁止依赖其他业务服务的实现模块。
- `knowledge-components` 仅作为 Maven 聚合父模块；业务服务必须按需依赖具体子模块，
  不得直接依赖聚合 POM，也不得把多个无关技术能力合并成万能基础设施 Jar。
- `knowledge-api` 中的契约按消费者需求设计，避免直接暴露数据库实体。
- 公共模块不放业务枚举、业务表结构、Mapper 或 Controller。
- 禁止循环依赖；使用 Maven Enforcer 和 ArchUnit 作为自动门禁。
- 服务先按业务域分包，再采用 `{domain}/controller/service/dao/mapper/model/dto/bo/vo/converter/config/mq/job/rule/enums` 的传统分层；DO 位于 `{domain}/dao/model`，Mapper 位于 `{domain}/dao/mapper`，不创建 `domain/application/interfaces/infrastructure` DDD 分层。
- Controller 不开启事务、不写 SQL、不直接操作 Redis/MQ；Service 定义事务边界；Mapper 只做数据访问。
- 持久化对象使用 `DO` 后缀，不得越过 Service 暴露给 Controller；接口输入使用 DTO，Service 业务结果使用 BO，Controller 输出使用 VO。四类对象严格分离并通过手写 Converter 转换。
- 业务 Spring Bean 的依赖统一使用 `@Resource` 注入；同类型多 Bean 时按显式 Bean 名称消歧。配置属性继续使用 `@Value` 或 `@ConfigurationProperties`；可选能力优先使用条件装配和 Noop 实现，确有运行时重配置需求时使用明确的 Setter 注入。`@Bean` 使用方法参数注入；禁止业务组件使用 `@Autowired` 和 `ObjectProvider` 延迟查找。

## 3. API 约束

- 具体 HTTP 规范遵循 `docs/08-api-infrastructure.md`；Java 编码与工程质量同时参考阿里巴巴 Java 开发手册。
- 外部接口前缀为 `/api`，内部接口前缀为 `/internal`；URL 暂不携带版本号，不兼容升级必须先通过 ADR 明确版本策略。
- URL 使用资源名词；状态变更使用明确动作端点或符合语义的 HTTP 方法。
- 金额使用最小货币单位的整数或 `BigDecimal`，禁止使用 `double`。
- ID 在服务边界以字符串传递，避免 JavaScript 精度问题。
- 时间存储使用 UTC，接口使用 ISO-8601 并携带时区。
- 分页必须限制最大页大小；批量接口必须限制最大元素数。
- 错误响应至少包含 `code`、`message`、`requestId`，业务错误码稳定且可检索。
- 普通 JSON 成功和错误响应显式使用 `Result<T>`；HTTP 状态表达结果类别，响应体 `code` 与之保持一致，禁止用 HTTP 200 承载失败。
- `204`、文件、图片、流式下载和 SSE 不使用 JSON 包装；不得用全局 `ResponseBodyAdvice` 隐式改变 Controller 契约。

## 4. 数据库约束

- 每个服务独占数据库或 schema，其他服务只能通过 API/事件访问数据。
- 独立 schema 已承担业务命名空间，物理表名不再添加 `mk_`、`lr_`、`pt_`、`iam_` 等服务缩写前缀；表名使用小写下划线并表达完整业务对象，如 `group_order`、`video_progress`、`point_account`、`user_account`。
- 表使用业务无关主键，并为业务幂等键建立唯一索引。
- 状态字段使用受控枚举；金额、数量、版本号不得为 null。
- 结构变更使用版本化迁移脚本，禁止依赖应用启动自动改表。
- 更新关键状态时使用条件更新或乐观锁，检查受影响行数。
- 查询必须有上界；禁止无条件全表扫描和无界导出。

## 5. Redis 与分布式锁

- Key 格式：`kw:{service}:{domain}:{resource}:{businessIdentifiers}`；固定前缀由所属业务域的 Key 工厂统一生成，禁止在业务代码中散落字符串拼接。
- Key 只使用小写英文层级和受校验的业务 ID，不写入手机号、邮箱、令牌等敏感信息；结构不兼容时增加版本段。
- 必须为 Key 记录 Redis 类型、Value/Member 格式、TTL 和清理策略；永久 Key 必须在设计文档中说明。
- Lua 同时操作多个 Key 时必须使用 Redis Cluster Hash Tag 保证同槽，或在 ADR 中明确仅支持单机 Redis。
- Redis 用于加速、短期占位和协调，不作为订单、权益等唯一事实源。
- 分布式锁必须包含唯一持有者标识，并只释放自己的锁。
- 锁的租约、等待时间、失败行为和临界区必须明确；持锁期间禁止慢速远程调用。
- 对强一致关键写入，锁之外仍使用数据库唯一约束或条件更新兜底。

## 6. 消息约束

- 事件名称使用已经发生的事实，如 `GroupFormed`，不用含糊命令名。
- 事件包含 `eventId`、`eventType`、`occurredAt`、`aggregateId`、`version` 和业务负载。
- 生产端使用事务 Outbox 保证本地状态与待发送事件一致。
- 消费者以 `eventId + consumer` 建立幂等记录，成功提交业务事务后再确认消息。
- 重试必须有次数与退避上限；超过上限进入死信并告警，支持人工重放。
- 事件 schema 只做向后兼容演进；删除或改变字段语义需要新版本。

## 7. 并发与幂等

- 拼团参与的幂等键：`groupId + userId`；同一用户对同一团终身只能产生一条参与记录和一张交易订单。
- 支付通知的幂等键：支付渠道流水号；订单状态采用合法状态迁移与条件更新。
- 权益发放唯一键：`sourceType + sourceId + userId + courseId`。
- 所有线程池使用有界队列、业务命名、明确拒绝策略和监控；禁止直接使用公共线程池执行核心任务。

## 8. 可观测性

- 所有入口生成或透传 `requestId`/`traceId`。
- 日志使用结构化字段，至少包含服务名、环境、请求 ID、业务主键和错误码。
- 核心指标包括请求量、错误率、P95/P99、线程池、连接池、MQ 积压和补偿任务数量。
- 健康检查区分存活与就绪；依赖不可用时不得谎报就绪。
- 业务审计与技术日志分离，关键状态迁移可追溯。

## 9. 配置与安全

- 密钥只通过环境变量或密钥服务注入，仓库仅提交示例配置。
- 本地、测试、生产配置分层，生产不得继承不安全的本地默认值。
- 认证只证明身份，资源级授权仍由业务服务执行。
- 上传、搜索、分页和批量参数设置大小上限；数据库访问使用参数绑定。
- 依赖漏洞扫描发现高危漏洞时阻断发布或记录有期限的例外。

## 10. 禁止事项

- 禁止跨服务数据库 JOIN、共享表和共享事务。
- 禁止在 Controller 中直接写核心业务规则，禁止形成无边界的“万能 Service”。
- 禁止捕获异常后只打印日志并返回成功。
- 禁止无限重试、无限队列、无限分页和无 TTL 缓存。
- 禁止把压测数据、规划能力或第三方示例代码包装成个人已完成成果。
