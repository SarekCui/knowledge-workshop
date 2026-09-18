package com.knowledge.agent.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateConversationDTO(@NotBlank @Size(max = 120) String title) {
}
