package com.knowledge.points.ranking.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledge.api.points.dto.PointGrantEventDTO;
import com.knowledge.points.ranking.bo.PointGrantResultBO;
import com.knowledge.points.ranking.config.PointsRabbitConfiguration;
import java.io.IOException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class PointGrantConsumer {

    private final ObjectMapper objectMapper;
    private final PointGrantTransactionService transactionService;
    private final LeaderboardService leaderboardService;

    public PointGrantConsumer(ObjectMapper objectMapper, PointGrantTransactionService transactionService,
                              LeaderboardService leaderboardService) {
        this.objectMapper = objectMapper;
        this.transactionService = transactionService;
        this.leaderboardService = leaderboardService;
    }

    @RabbitListener(queues = PointsRabbitConfiguration.POINT_GRANT_QUEUE)
    public void consume(String payload) throws IOException {
        PointGrantEventDTO event = objectMapper.readValue(payload, PointGrantEventDTO.class);
        PointGrantResultBO result = transactionService.grant(event);
        if (result == null) {
            throw new IllegalStateException("积分事件已存在，但赛季账户不存在: " + event.eventId());
        }
        leaderboardService.synchronizeScore(result.season(), event.userId(), result.seasonPoints());
    }
}
