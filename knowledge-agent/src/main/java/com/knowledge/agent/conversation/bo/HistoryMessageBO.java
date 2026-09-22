package com.knowledge.agent.conversation.bo;

import java.time.LocalDateTime;

public record HistoryMessageBO(String id, String role, String content, LocalDateTime createdAt) {
}
