# 第5.5轮：断点续播并发与故障加固

## 实现

1. 创建会话：一次Lua生成代次、替换会话Hash、设置代次30天与会话2小时TTL。会话校验和续期也由单次Lua完成，旧会话不能续期新会话。
2. 会话Key升级为 `kw:learning:progress:session:v2:{userId:videoId}` 与 `kw:learning:progress:session-epoch:v2:{userId:videoId}`。两个Key同槽；旧Key不读取，按原TTL自然过期，升级时已有播放器须重新创建会话。不删除用户数据或重命名现有快照Key。
3. 断点读取：Redis读失败、数值/时间/必需字段损坏时读取MySQL，回填失败仍返回已落库断点。最近学习索引不存在或Redis故障时使用有界同库查询，过滤失效权益、下架视频、旧版本与未发生学习的空进度。
4. 所有上报附加数据库读取移到消息发布之前。确认成功后只尝试缓存更新，不再调用数据库，从而避免已确认事件却因后续回源错误要求客户端重发。
5. 开启 Publisher Returns 和 mandatory：ACK但消息被退回不可路由也视为失败。NACK、连接失败、确认异常或超时返回503。确认默认等待5000ms，可用 `knowledge.learning.progress.confirm-timeout-ms` 配置；RabbitMQ连接超时5秒，Redis连接及命令超时1秒。
6. 上报的乐观快照只接受严格更新的代次/序号，不能以相同序号覆盖消费者已写入的持久化完成度；消费者及回源仍允许相同代次/序号刷新数据库投影。

## 响应及重试约定

- `accepted=true`：MQ已确认接收且未退回，不表示MySQL落库，不表示该位置一定赢得事件排序。
- `cacheUpdated=true`：本次乐观缓存更新调用成功。false可能表示排序未胜出、消费者已先写入相同序号，或缓存更新失败/结果不确定；前端不能据此用响应位置覆盖本地播放器。
- MQ已确认后缓存失败仍成功返回，后续消费落库及回源恢复。
- 超时结果不确定，事件可能已经进入MQ。客户端保持原 eventId、会话、sequence、位置及观看区间重试，不生成新事件。Inbox唯一键防重复消费。
- 会话已被新设备替换返回409，旧会话不能无限重试；应重新开播并读取已保存断点，不擅自将旧事件换为新会话事件。
- Redis完全不可用时允许查询MySQL，但创建/校验会话安全失败并返回503，不绕过鉴权和会话控制。
- 本轮仍不增加Outbox，不声称跨MQ/Redis/MySQL事务或RabbitMQ exactly-once，也不声称近期未消费事件已能从MySQL立即恢复。

## 验证

- ProgressEventPublisherTest：ACK、NACK、不可路由返回、确认超时与连接失败。
- ProgressQueryServiceTest：Redis离线与回填失败仍返回MySQL断点、快照损坏、最近列表有界回源。
- ProgressServiceTest：确认后缓存失败仍接受，数据库读取在确认前，发布失败不推进缓存。
- LearningAcceptanceIT：真实Redis32次并发开播后Hash与代次Key一致且保留最高代次、旧会话拒绝、TTL；真实RabbitMQ删除绑定导致不可路由、恢复后原事件重试只入账一次；损坏Redis字段和清空索引后HTTP恢复已落库位置。
- Redis离线、MQ确认超时、确认后缓存失败使用单元替身注入；未声称这些全部是容器断网演练。已有消息消费幂等、乱序、倒退、多设备及课程进度回归继续运行。

## 执行结果

2026-09-14 执行 `JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home mvn -Dmaven.repo.local=.m2/repository -Pacceptance verify` 通过。全仓库53个单元测试、38个集成验收测试通过，无失败、无跳过。本轮新增10个单元测试、3个真实容器验收场景；学习模块13个单元测试、12个容器验收通过。结果是正确性回归，不是生产性能压测。未启动长期运行的前端或业务服务。
