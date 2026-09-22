package com.knowledge.agent.conversation.bo;

import java.time.LocalDateTime;

public record ConversationBO(String id, String title, int version, LocalDateTime createdAt) {
}
