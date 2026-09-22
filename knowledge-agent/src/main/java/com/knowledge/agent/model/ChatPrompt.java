package com.knowledge.agent.model;

public record ChatPrompt(
        String systemPrompt,
        String userPrompt,
        String userId) {
}
