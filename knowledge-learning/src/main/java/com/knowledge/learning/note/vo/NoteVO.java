package com.knowledge.learning.note.vo;

import java.time.LocalDateTime;

public record NoteVO(
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
