package com.knowledge.api.learning.dto;

import java.time.Instant;

/**
 * 公开 Note 评论中明确提及“小智”后，由 learning 发布给 Agent 服务的领域事件。
 * 事件仅携带稳定标识，正文由消费者通过受鉴权内部接口重新读取。
 */
public record AgentMentionedEventDTO(
        String eventId,
        String eventType,
        Instant occurredAt,
        String aggregateId,
        int version,
        String noteId,
        String commentId,
        String parentCommentId,
        String requesterId) {
}
