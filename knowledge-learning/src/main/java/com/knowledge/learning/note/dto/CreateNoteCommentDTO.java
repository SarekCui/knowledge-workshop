package com.knowledge.learning.note.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateNoteCommentDTO(
        @NotBlank @Size(max = 128) String clientRequestId,
        @Size(max = 64) String parentCommentId,
        @NotBlank @Size(max = 1000) String content) {
}
