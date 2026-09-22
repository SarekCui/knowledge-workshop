package com.knowledge.agent.model.service;

import com.knowledge.agent.model.ChatPrompt;
import com.knowledge.agent.model.GenerationResult;
import com.knowledge.agent.model.StreamingChatModelClient;
import com.knowledge.common.exception.BusinessException;

import java.util.function.Consumer;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Default model client used when the optional model integration is disabled.
 */
@Service
@ConditionalOnProperty(name = "knowledge.agent.model.enabled", havingValue = "false", matchIfMissing = true)
public class UnavailableStreamingChatModelClient implements StreamingChatModelClient {

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public void stream(
            ChatPrompt request,
            Consumer<String> onDelta,
            Consumer<GenerationResult> onComplete,
            Consumer<Throwable> onError
    ) {
        throw BusinessException.serviceUnavailable("小智模型尚未配置");
    }
}
