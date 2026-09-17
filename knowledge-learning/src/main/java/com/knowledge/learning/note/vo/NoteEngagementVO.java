package com.knowledge.learning.note.vo;

public record NoteEngagementVO(
        long likeCount,
        long favoriteCount,
        long commentCount,
        boolean liked,
        boolean favorited) {
}
