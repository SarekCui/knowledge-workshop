package com.knowledge.learning.note.dto;

import com.knowledge.learning.note.enums.NoteStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ChangeNoteStatusDTO(@NotNull NoteStatus status, @PositiveOrZero int version) {
}
