package com.knowledge.agent.model;

import java.util.function.Consumer;

/** Streaming generation port. Implementations must never expose model thinking content. */
public interface AgentStreamingModelService {

    void stream(AgentModelRequest request, Consumer<String> onDelta,
            Consumer<AgentModelResponse> onComplete, Consumer<Throwable> onError);
}
