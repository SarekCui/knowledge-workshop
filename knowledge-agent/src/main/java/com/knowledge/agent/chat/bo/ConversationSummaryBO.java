package com.knowledge.agent.chat.bo;

import java.time.LocalDateTime;

public record ConversationSummaryBO(String id, String title, LocalDateTime updatedAt) {
}
