package com.knowledge.agent.chat.bo;

import java.time.LocalDateTime;

public record AgentHistoryMessageBO(String id, String role, String content, LocalDateTime createdAt) {
}
