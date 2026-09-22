package com.knowledge.agent.model.service;

import com.knowledge.agent.model.ChatPrompt;
import com.knowledge.agent.model.GenerationResult;
import com.knowledge.agent.model.ChatModelClient;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import jakarta.annotation.Resource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnBean(ChatModel.class)
public class DeepSeekChatModelClient implements ChatModelClient {

    @Resource
    private ChatModel chatModel;

    @Override
    public GenerationResult generate(ChatPrompt request) {
        ChatResponse response = chatModel.chat(SystemMessage.from(request.systemPrompt()),
                UserMessage.from(request.userPrompt()));
        return new GenerationResult(response.aiMessage().text(), response.tokenUsage().inputTokenCount(),
                response.tokenUsage().outputTokenCount());
    }
}
