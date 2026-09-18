package com.knowledge.learning.note.vo;

import java.time.LocalDateTime;

public record NoteCommentVO(
        String id,
        String noteId,
        String authorId,
        String authorType,
        String parentCommentId,
        String sourceCommentId,
        String content,
        long likeCount,
        boolean liked,
        boolean owned,
        int version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
