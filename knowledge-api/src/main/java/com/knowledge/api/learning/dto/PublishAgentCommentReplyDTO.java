package com.knowledge.api.learning.dto;

/** Internal contract: Agent publishes an idempotent reply to a public Note comment. */
public record PublishAgentCommentReplyDTO(
        String clientRequestId,
        String noteId,
        String sourceCommentId,
        String content) {
}
