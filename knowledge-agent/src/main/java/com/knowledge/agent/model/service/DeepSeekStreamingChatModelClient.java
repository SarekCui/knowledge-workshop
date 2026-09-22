package com.knowledge.agent.model.service;

import com.knowledge.agent.model.ChatPrompt;
import com.knowledge.agent.model.GenerationResult;
import com.knowledge.agent.model.StreamingChatModelClient;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import java.util.List;
import java.util.function.Consumer;
import jakarta.annotation.Resource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnBean(StreamingChatModel.class)
@ConditionalOnProperty(name = "knowledge.agent.model.enabled", havingValue = "true")
public class DeepSeekStreamingChatModelClient implements StreamingChatModelClient {

    @Resource private StreamingChatModel chatModel;


    @Override
    public void stream(
            ChatPrompt request,
            Consumer<String> onDelta,
            Consumer<GenerationResult> onComplete,
            Consumer<Throwable> onError
    ) {
        chatModel.chat(List.of(SystemMessage.from(request.systemPrompt()), UserMessage.from(request.userPrompt())),
                new StreamingChatResponseHandler() {
                    @Override
                    public void onPartialResponse(String partialResponse) {
                        onDelta.accept(partialResponse);
                    }

                    @Override
                    public void onCompleteResponse(ChatResponse response) {
                        onComplete.accept(new GenerationResult(response.aiMessage().text(),
                                response.tokenUsage().inputTokenCount(), response.tokenUsage().outputTokenCount()));
                    }

                    @Override
                    public void onError(Throwable error) {
                        onError.accept(error);
                    }
                });
    }
}
