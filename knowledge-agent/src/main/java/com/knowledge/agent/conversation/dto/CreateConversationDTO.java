package com.knowledge.agent.conversation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateConversationDTO(@NotBlank @Size(max = 120) String title) {
}
