package com.knowledge.agent.model;

public record GenerationResult(
        String text,
        Integer inputTokens,
        Integer outputTokens) {
}
