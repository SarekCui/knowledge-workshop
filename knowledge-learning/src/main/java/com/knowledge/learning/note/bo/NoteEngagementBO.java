package com.knowledge.learning.note.bo;

public record NoteEngagementBO(
        long likeCount,
        long favoriteCount,
        long commentCount,
        boolean liked,
        boolean favorited) {
}
