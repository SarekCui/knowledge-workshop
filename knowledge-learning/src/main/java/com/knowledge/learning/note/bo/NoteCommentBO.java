package com.knowledge.learning.note.bo;

import java.time.LocalDateTime;

public record NoteCommentBO(
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
