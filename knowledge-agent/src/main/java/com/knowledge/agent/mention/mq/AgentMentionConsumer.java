package com.knowledge.agent.mention.mq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledge.agent.mention.config.AgentRabbitConfiguration;
import com.knowledge.agent.mention.service.AgentMentionRunService;
import com.knowledge.api.learning.dto.AgentMentionedEventDTO;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AgentMentionConsumer {

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private AgentMentionRunService runService;

    @RabbitListener(queues = AgentRabbitConfiguration.AGENT_MENTION_QUEUE)
    public void consume(String payload) {
        try {
            AgentMentionedEventDTO event = objectMapper.readValue(payload, AgentMentionedEventDTO.class);
            validate(event);
            runService.enqueue(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("无法解析小智提及事件", exception);
        }
    }

    private void validate(AgentMentionedEventDTO event) {
        if (event.eventId() == null || event.eventType() == null || event.commentId() == null
                || event.noteId() == null || event.requesterId() == null
                || !"AgentMentioned".equals(event.eventType())) {
            throw new IllegalArgumentException("小智提及事件不完整或类型非法");
        }
    }
}
