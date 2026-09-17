package com.knowledge.learning.note.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateNoteDTO(
        @NotBlank @Size(max = 128) String clientRequestId,
        @Size(max = 64) String courseId,
        @Size(max = 64) String chapterId,
        @NotBlank @Size(max = 100) String title,
        @NotNull @Size(max = 20000) String content,
        @PositiveOrZero Long videoPositionMs,
        @Size(max = 5) List<@NotBlank @Size(max = 20) String> tags) {

    public CreateNoteDTO(String clientRequestId, String courseId, String chapterId, String title,
            String content, Long videoPositionMs) {
        this(clientRequestId, courseId, chapterId, title, content, videoPositionMs, List.of());
    }
}
