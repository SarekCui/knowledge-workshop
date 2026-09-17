package com.knowledge.points.ranking.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledge.api.points.dto.PointGrantEventDTO;
import com.knowledge.points.ranking.bo.PointGrantResultBO;
import com.knowledge.points.ranking.config.PointsRabbitConfiguration;
import java.io.IOException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.knowledge.points.season.service.SeasonClosedException;

@Service
public class PointGrantConsumer {

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PointGrantTransactionService transactionService;
    @Autowired
    private LeaderboardService leaderboardService;
    @Autowired
    private RejectedPointEventService rejectedEventService;

    @RabbitListener(queues = PointsRabbitConfiguration.POINT_GRANT_QUEUE)
    public void consume(String payload) throws IOException {
        PointGrantEventDTO event = objectMapper.readValue(payload, PointGrantEventDTO.class);
        PointGrantResultBO result;
        try {
            result = transactionService.grant(event);
        } catch (SeasonClosedException exception) {
            // 持久化成功后才正常返回供 RabbitMQ ACK；保存失败仍抛出异常重试。
            rejectedEventService.retain(event.eventId(), payload, exception.getMessage());
            return;
        }
        if (result == null) {
            throw new IllegalStateException("积分事件已存在，但赛季账户不存在: " + event.eventId());
        }
        leaderboardService.synchronizeScore(result.season(), event.userId(), result.seasonPoints());
    }
}
