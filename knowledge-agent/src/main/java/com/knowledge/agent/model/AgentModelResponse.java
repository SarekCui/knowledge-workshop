package com.knowledge.agent.model;

public record AgentModelResponse(
        String text,
        Integer inputTokens,
        Integer outputTokens) {
}
