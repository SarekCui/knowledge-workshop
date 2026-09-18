package com.knowledge.agent.model;

public record AgentModelRequest(
        String systemInstruction,
        String userPrompt,
        String anonymousUserId) {
}
