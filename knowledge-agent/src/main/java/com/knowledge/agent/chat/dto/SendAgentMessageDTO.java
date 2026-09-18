package com.knowledge.agent.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SendAgentMessageDTO(
        @NotBlank @Pattern(regexp = "[0-9a-fA-F-]{36}") String clientRequestId,
        @NotBlank @Size(max = 6000) String question,
        @Size(max = 500) String pageContext) {
}
