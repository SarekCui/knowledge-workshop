package com.knowledge.learning.note.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateNoteDTO(
        @NotBlank @Size(max = 100) String title,
        @NotNull @Size(max = 20000) String content,
        @PositiveOrZero Long videoPositionMs,
        @NotNull @PositiveOrZero Integer version,
        @Size(max = 5) List<@NotBlank @Size(max = 20) String> tags) {

    public UpdateNoteDTO(String title, String content, Long videoPositionMs, Integer version) {
        this(title, content, videoPositionMs, version, List.of());
    }
}
