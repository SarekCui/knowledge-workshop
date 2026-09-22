package com.knowledge.agent.conversation.bo;

import java.time.LocalDateTime;

public record ConversationSummaryBO(String id, String title, LocalDateTime updatedAt) {
}
