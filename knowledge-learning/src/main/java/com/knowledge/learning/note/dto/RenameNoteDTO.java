package com.knowledge.learning.note.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record RenameNoteDTO(
        @NotBlank @Size(max = 100) String title,
        @NotNull @PositiveOrZero Integer version) {
}
