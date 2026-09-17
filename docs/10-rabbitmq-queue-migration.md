# 成团队列参数升级与保留消息迁移

## 原因与决策

旧 `learning.group-formed` 队列没有死信参数；RabbitMQ不允许重新声明同名队列时改变参数，会返回406并阻断消费者启动。
当前营销、学习服务统一声明 `learning.group-formed.v2`，成团事件的路由键仍是 `marketing.group.formed.v1`，事件Schema未变化；旧队列不自动删除。

## 操作

先停止营销生产者和学习消费者，确认无其他实例、旧队列消费者数和未确认数均为0。
新旧版本不能混合运行：旧服务会重新绑定旧队列，可能产生双投递；更新所有营销和学习实例后才能恢复生产。

在仓库根目录执行（依赖来自本项目Maven缓存）：

```bash
java --class-path .m2/repository/com/rabbitmq/amqp-client/5.25.0/amqp-client-5.25.0.jar:.m2/repository/org/slf4j/slf4j-api/2.0.17/slf4j-api-2.0.17.jar scripts/MigrateGroupFormedQueue.java
```

要求Java17。脚本默认连接本地RabbitMQ和项目开发账号；其他环境必须通过RABBITMQ_HOST/PORT/USERNAME/PASSWORD/VHOST覆盖，禁止在命令输出中打印凭据。

工具只迁移固定的两条命名队列，不接受任意删除目标：

1. 声明持久化的新队列、死信队列与绑定。
2. 先绑定新队列再解绑旧队列，避免无路由窗口；窗口内双投递由业务幂等兜底。
3. 使用手动确认获取旧消息，保留原正文、messageId和headers，设置持久化投递。
4. mandatory发布到新队列，收到publisher confirm且没有return后，才ack旧消息。
5. 确认超时、不可路由或连接失败则退出，未ack消息随连接关闭重新入旧队列。
6. 每次最多迁移10000条，不purge、不delete，旧队列保留以便检查。

确认后、旧ack前崩溃可能重复转发，消费者必须按eventId幂等；这是至少一次迁移，不宣称exactly-once。
消费规则不兼容或毒消息经过有限重试后进入 `learning.group-formed.dlq`，迁移成功不等于业务处理成功；不得盲目重放或改写历史消息。

确认与消费ack属于不同的安全边界，依据[官方RabbitMQ确认文档](https://www.rabbitmq.com/docs/confirms)。

## 本次证据

2026-09-14，旧队列2条待消费、0条未确认、0个消费者。迁移输出：

```text
迁移完成: transferred=2, sourceRemaining=0, targetReady=2
```

旧队列未删除。服务启动后另行检查正常队列、死信队列及业务事实，不能仅凭transfer计数宣称权益发放成功。

本次最终复核：旧队列0、新队列0、死信队列2，未确认消息0。两条历史消息经重试后保留在死信队列，尚未解决其消费失败原因；不能为了让学习列表非空而伪造业务事实。
