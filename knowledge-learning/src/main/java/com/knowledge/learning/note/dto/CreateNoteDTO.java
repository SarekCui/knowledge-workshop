package com.knowledge.learning.note.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CreateNoteDTO(
        @NotBlank @Size(max = 128) String clientRequestId,
        @NotBlank @Size(max = 64) String courseId,
        @Size(max = 64) String chapterId,
        @NotBlank @Size(max = 100) String title,
        @NotNull @Size(max = 20000) String content,
        @PositiveOrZero Long videoPositionMs) {
}
