package com.knowledge.learning.note.bo;

import java.time.LocalDateTime;
import com.knowledge.learning.note.enums.NoteStatus;
import java.util.List;

public record NoteBO(
        String id,
        String courseId,
        String chapterId,
        String title,
        String content,
        Long videoPositionMs,
        int version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String authorId,
        NoteStatus status,
        LocalDateTime publishedAt,
        long likeCount,
        long favoriteCount,
        long commentCount,
        List<String> tags,
        boolean liked,
        boolean favorited) {
}
