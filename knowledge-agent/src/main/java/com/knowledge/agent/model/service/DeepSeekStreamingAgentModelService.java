package com.knowledge.agent.model.service;

import com.knowledge.agent.model.AgentModelRequest;
import com.knowledge.agent.model.AgentModelResponse;
import com.knowledge.agent.model.AgentStreamingModelService;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import java.util.List;
import java.util.function.Consumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnBean(StreamingChatModel.class)
public class DeepSeekStreamingAgentModelService implements AgentStreamingModelService {

    @Autowired
    private StreamingChatModel chatModel;

    @Override
    public void stream(AgentModelRequest request, Consumer<String> onDelta,
            Consumer<AgentModelResponse> onComplete, Consumer<Throwable> onError) {
        chatModel.chat(List.of(SystemMessage.from(request.systemInstruction()), UserMessage.from(request.userPrompt())),
                new StreamingChatResponseHandler() {
                    @Override
                    public void onPartialResponse(String partialResponse) {
                        onDelta.accept(partialResponse);
                    }

                    @Override
                    public void onCompleteResponse(ChatResponse response) {
                        onComplete.accept(new AgentModelResponse(response.aiMessage().text(),
                                response.tokenUsage().inputTokenCount(), response.tokenUsage().outputTokenCount()));
                    }

                    @Override
                    public void onError(Throwable error) {
                        onError.accept(error);
                    }
                });
    }
}
