package com.knowledge.learning.note.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 创建笔记请求。
 *
 * @param idempotencyKey 逻辑写操作的稳定幂等键，格式为 {@code 调用方:操作:稳定标识}，例如
 *                       {@code web:note-create:550e8400-e29b-41d4-a716-446655440000}。
 *                       调用方在首次发起操作时生成；网络超时或响应未知时必须原样复用，
 *                       用户修改内容后重新提交则必须生成新键。
 */
public record CreateNoteDTO(
        @NotBlank @Size(max = 128) String idempotencyKey,
        @Size(max = 64) String courseId,
        @Size(max = 64) String chapterId,
        @NotBlank @Size(max = 100) String title,
        @NotNull @Size(max = 20000) String content,
        @PositiveOrZero Long videoPositionMs,
        @Size(max = 5) List<@NotBlank @Size(max = 20) String> tags) {

    public CreateNoteDTO(String idempotencyKey, String courseId, String chapterId, String title,
            String content, Long videoPositionMs) {
        this(idempotencyKey, courseId, chapterId, title, content, videoPositionMs, List.of());
    }
}
