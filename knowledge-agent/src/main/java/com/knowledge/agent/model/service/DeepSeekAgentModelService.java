package com.knowledge.agent.model.service;

import com.knowledge.agent.model.AgentModelRequest;
import com.knowledge.agent.model.AgentModelResponse;
import com.knowledge.agent.model.AgentModelService;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnBean(ChatModel.class)
public class DeepSeekAgentModelService implements AgentModelService {

    @Autowired
    private ChatModel chatModel;

    @Override
    public AgentModelResponse generate(AgentModelRequest request) {
        ChatResponse response = chatModel.chat(SystemMessage.from(request.systemInstruction()),
                UserMessage.from(request.userPrompt()));
        return new AgentModelResponse(response.aiMessage().text(), response.tokenUsage().inputTokenCount(),
                response.tokenUsage().outputTokenCount());
    }
}
