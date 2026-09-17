package com.knowledge.tools;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/** One-off operator tool: confirmed transfer, never purge/delete the legacy queue. */
public class MigrateGroupFormedQueue {
    private static final String SOURCE = "learning.group-formed";
    private static final String TARGET = "learning.group-formed.v2";
    private static final String EXCHANGE = "knowledge.events";
    private static final String DEAD_EXCHANGE = "knowledge.events.dlx";
    private static final String ROUTING_KEY = "marketing.group.formed.v1";

    public static void main(String[] args) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(System.getenv().getOrDefault("RABBITMQ_HOST", "127.0.0.1"));
        factory.setPort(Integer.parseInt(System.getenv().getOrDefault("RABBITMQ_PORT", "5672")));
        factory.setUsername(System.getenv().getOrDefault("RABBITMQ_USERNAME", "knowledge"));
        factory.setPassword(System.getenv().getOrDefault("RABBITMQ_PASSWORD", "knowledge"));
        factory.setVirtualHost(System.getenv().getOrDefault("RABBITMQ_VHOST", "/"));
        factory.setAutomaticRecoveryEnabled(false);
        factory.setConnectionTimeout(5000);
        try (Connection connection = factory.newConnection("group-formed-queue-migration");
             Channel channel = connection.createChannel()) {
            var source = channel.queueDeclarePassive(SOURCE);
            if (source.getConsumerCount() != 0) throw new IllegalStateException("请先停止旧队列消费者");
            channel.exchangeDeclare(EXCHANGE, "direct", true);
            channel.exchangeDeclare(DEAD_EXCHANGE, "direct", true);
            channel.queueDeclare("learning.group-formed.dlq", true, false, false, null);
            channel.queueBind("learning.group-formed.dlq", DEAD_EXCHANGE, ROUTING_KEY);
            channel.queueDeclare(TARGET, true, false, false, Map.of(
                    "x-dead-letter-exchange", DEAD_EXCHANGE, "x-dead-letter-routing-key", ROUTING_KEY));
            channel.queueBind(TARGET, EXCHANGE, ROUTING_KEY);
            // Bind new first, then disconnect old routing. Crash-window duplicates are consumer-idempotent.
            channel.queueUnbind(SOURCE, EXCHANGE, ROUTING_KEY);
            channel.confirmSelect();
            AtomicBoolean returned = new AtomicBoolean();
            channel.addReturnListener(value -> returned.set(true));
            int transferred = 0;
            GetResponse message;
            while ((message = channel.basicGet(SOURCE, false)) != null) {
                if (transferred >= 10000) throw new IllegalStateException("迁移超过上限，停止并保留剩余消息");
                returned.set(false);
                channel.basicPublish("", TARGET, true,
                        message.getProps().builder().deliveryMode(2).build(), message.getBody());
                channel.waitForConfirmsOrDie(5000);
                if (returned.get()) throw new IllegalStateException("目标队列不可路由，旧消息未确认");
                channel.basicAck(message.getEnvelope().getDeliveryTag(), false);
                transferred++;
            }
            int remaining = channel.queueDeclarePassive(SOURCE).getMessageCount();
            int target = channel.queueDeclarePassive(TARGET).getMessageCount();
            System.out.printf("迁移完成: transferred=%d, sourceRemaining=%d, targetReady=%d%n",
                    transferred, remaining, target);
            if (remaining != 0) throw new IllegalStateException("旧队列仍有消息，需要继续检查");
        }
    }
}
