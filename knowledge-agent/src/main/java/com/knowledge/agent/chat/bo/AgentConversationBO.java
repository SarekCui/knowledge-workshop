package com.knowledge.agent.chat.bo;

import java.time.LocalDateTime;

public record AgentConversationBO(String id, String title, int version, LocalDateTime createdAt) {
}
