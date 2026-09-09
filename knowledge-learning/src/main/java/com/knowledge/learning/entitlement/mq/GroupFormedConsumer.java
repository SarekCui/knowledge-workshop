package com.knowledge.learning.entitlement.mq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledge.api.marketing.dto.GroupFormedEventDTO;
import com.knowledge.learning.entitlement.config.LearningRabbitConfiguration;
import com.knowledge.learning.entitlement.service.EntitlementService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class GroupFormedConsumer {

    private final ObjectMapper objectMapper;
    private final EntitlementService entitlementService;

    public GroupFormedConsumer(ObjectMapper objectMapper, EntitlementService entitlementService) {
        this.objectMapper = objectMapper;
        this.entitlementService = entitlementService;
    }

    @RabbitListener(queues = LearningRabbitConfiguration.GROUP_FORMED_QUEUE)
    public void consume(String payload) {
        try {
            entitlementService.grantFromGroup(objectMapper.readValue(payload, GroupFormedEventDTO.class));
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("无法解析成团事件", exception);
        }
    }
}
