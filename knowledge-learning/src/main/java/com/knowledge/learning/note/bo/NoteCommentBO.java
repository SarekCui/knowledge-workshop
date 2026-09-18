package com.knowledge.learning.note.bo;

import com.knowledge.learning.note.enums.NoteCommentAuthorType;
import java.time.LocalDateTime;

public record NoteCommentBO(
        String id,
        String noteId,
        String authorId,
        NoteCommentAuthorType authorType,
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
