package com.knowledge.learning.note.bo;

import java.time.LocalDateTime;

public record NoteBO(
        String id,
        String courseId,
        String chapterId,
        String title,
        String content,
        Long videoPositionMs,
        int version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
