package com.knowledge.learning.note.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record UpdateNoteDTO(
        @NotBlank @Size(max = 100) String title,
        @NotNull @Size(max = 20000) String content,
        @PositiveOrZero Long videoPositionMs,
        @NotNull @PositiveOrZero Integer version) {
}
