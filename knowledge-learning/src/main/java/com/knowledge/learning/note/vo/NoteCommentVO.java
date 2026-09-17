package com.knowledge.learning.note.vo;

import java.time.LocalDateTime;

public record NoteCommentVO(
        String id,
        String noteId,
        String authorId,
        String parentCommentId,
        String content,
        boolean owned,
        int version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
