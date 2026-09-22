package com.knowledge.api.learning.dto;

/**
 * Internal, read-only context for composing an answer to a public Note comment.
 * The learning service validates the Note and comment visibility before returning it.
 */
public record AgentCommentContextDTO(
        String noteTitle,
        String noteContent,
        String commentContent) {
}
