package com.knowledge.agent.model;

import java.util.function.Consumer;

/** Streaming generation port. Implementations must never expose model thinking content. */
public interface StreamingChatModelClient {

    default boolean isAvailable() {
        return true;
    }

    void stream(ChatPrompt request, Consumer<String> onDelta,
            Consumer<GenerationResult> onComplete, Consumer<Throwable> onError);
}
