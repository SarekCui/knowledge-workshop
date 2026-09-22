package com.knowledge.learning.entitlement.mq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledge.api.marketing.dto.GroupFormedEventDTO;
import com.knowledge.learning.entitlement.config.LearningRabbitConfiguration;
import com.knowledge.learning.entitlement.service.EntitlementService;
import jakarta.annotation.Resource;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class GroupFormedConsumer {

    @Resource private ObjectMapper objectMapper;
    @Resource private EntitlementService entitlementService;


    @RabbitListener(queues = LearningRabbitConfiguration.GROUP_FORMED_QUEUE)
    public void consume(String payload) {
        try {
            entitlementService.grantFromGroup(objectMapper.readValue(payload, GroupFormedEventDTO.class));
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("无法解析成团事件", exception);
        }
    }
}
