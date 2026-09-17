# Knowledge Workshop Engineering Rules

本文件约束本仓库后续所有人工与 Agent 变更。

## 变更前

- 先阅读 `docs/00-product-scope.md` 至 `docs/05-quality-and-acceptance.md`。
- 变更必须属于 `docs/04-iteration-roadmap.md` 中当前轮次；跨轮次需求先更新路线图。
- 新增服务、数据库、中间件或同步依赖前，必须新增或更新 ADR。
- 不得仅为“体现微服务”拆分服务；服务边界必须对应独立业务能力和数据所有权。

## 代码结构

- Java 基础包必须使用 `com.knowledge`，服务代码位于 `com.knowledge.{service}`；不得使用个人标识作为包路径。
- 服务包先按业务域拆分，再在域内使用传统分层：`{domain}.controller -> {domain}.service -> {domain}.dao.mapper`；数据库映射对象位于 `{domain}.dao.model`，辅助目录为 `dto`、`bo`、`vo`、`converter`、`config`、`mq`、`job`、`rule`、`enums`。
- Controller 只负责协议转换、参数校验和调用 Service；不得承载事务与核心业务判断。
- 新增和修改业务组件使用 `@Autowired` 字段注入；基础设施 `@Bean` 保留方法参数注入，同类型多 Bean 无法消歧时再使用 `@Qualifier`。
- Service 负责业务编排和事务边界；复杂校验拆到 `rule` 责任链，通用技术能力拆到独立 Starter。
- Mapper 只负责数据访问，不得调用远程服务、MQ 或 Redis。
- 持久化对象统一使用 `DO` 后缀，接口输入和跨服务传输对象统一使用 `DTO` 后缀，Service 业务结果统一使用 `BO` 后缀，Controller 对外展示对象统一使用 `VO` 后缀。
- Controller 不得直接接收或返回 DO；DTO、DO、BO、VO 之间必须通过显式 Converter 转换，不使用反射式 BeanUtils。
- 禁止把所有 Controller、Service、Mapper 或 DO 挤入服务级同名目录；跨业务域调用通过对方 Service，不直接访问对方 Mapper。
- `knowledge-api` 只存放跨服务契约，不存放业务实现或持久化实体。
- `knowledge-common` 只容纳稳定、通用、无业务语义的能力；禁止成为杂物模块。
- 可复用技术组件由 `knowledge-components` 聚合；业务服务按需依赖具体组件，禁止依赖聚合 POM，部署设施仍位于根目录 `infra`。
- 服务之间禁止直接访问对方数据库，禁止共享持久化实体。

## 前端约束

- 前端使用 React、TypeScript 和 Ant Design；只实现接入实际业务接口的功能，不添加无用 Demo 或 Mock 页面。
- 按业务能力拆分组件；播放器事件和重试逻辑由业务 Hook / 独立模块管理，展示组件不承载请求编排。
- 跨组件共享状态和方法通过 Context 与业务 Hook 获取，不通过多层组件参数传递；Context 限定在业务范围，禁止成为全局杂物容器。
- 高频播放状态放入 ref；Effect 必须清理事件、定时器及异步订阅。接口失败、会话冲突、媒体加载失败必须可见。
- TypeScript 严格检查、前端测试、生产构建和浏览器验收通过后才能标记前端能力完成。

## 数据与一致性

- 所有写接口必须定义幂等键或明确说明为何不需要。
- 库存、名额、余额、积分等计数变更必须说明并发控制与失败恢复策略。
- 本地事务只覆盖单服务数据库；跨服务使用事件、Outbox、补偿和对账。
- 消息消费按“至少一次”设计，消费者必须幂等；禁止假设 MQ 提供业务 exactly-once。
- 缓存不是事实源。缓存删除、重建、过期和降级路径必须可解释。

## API 与安全

- 外部 API 统一经网关进入；内部接口和外部接口分开命名与鉴权。
- 使用统一错误码、请求 ID 和时间格式，不向调用方暴露堆栈或内部异常。
- 资源级权限在服务端校验，不能只依赖网关。
- 日志不得记录密码、完整令牌、支付敏感信息和不必要的个人数据。

## 测试与交付

- 业务规则和状态迁移必须有单元测试；数据库、Redis、RabbitMQ 交互必须有集成测试。
- 修复缺陷时先增加能够复现问题的测试。
- 提交前至少运行当前模块测试；完成轮次前运行全量构建和关键链路验收。
- 未通过 `docs/05-quality-and-acceptance.md` 的能力不得标记为完成，也不得写入简历成果。
- 不提交密钥、真实账号、本地 IDE 文件、构建产物和大体积运行数据。
