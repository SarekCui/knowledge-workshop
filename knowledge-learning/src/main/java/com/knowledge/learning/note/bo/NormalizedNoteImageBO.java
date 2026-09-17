package com.knowledge.learning.note.bo;

public record NormalizedNoteImageBO(
        byte[] content,
        String contentType,
        String extension,
        int width,
        int height) {
}
