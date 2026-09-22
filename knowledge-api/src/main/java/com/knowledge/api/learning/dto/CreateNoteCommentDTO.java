package com.knowledge.api.learning.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建公开 Note 评论或回复的跨服务契约。
 *
 * @param idempotencyKey 逻辑写操作的稳定幂等键，格式为 {@code 调用方:操作:稳定标识}，例如
 *                       {@code web:comment-create:550e8400-e29b-41d4-a716-446655440000}。
 *                       同一次评论或回复的重试必须复用原键；不能把 {@code X-Request-Id}
 *                       当作幂等键。
 * @param parentCommentId 被回复的评论 ID；为空时创建主评论。
 * @param content 评论正文。
 */
public record CreateNoteCommentDTO(
        @NotBlank @Size(max = 128) String idempotencyKey,
        @Size(max = 64) String parentCommentId,
        @NotBlank @Size(max = 1000) String content) {
}
