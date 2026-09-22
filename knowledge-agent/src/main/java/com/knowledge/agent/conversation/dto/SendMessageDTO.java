package com.knowledge.agent.conversation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 发起一轮小智问答的请求。
 *
 * @param idempotencyKey 逻辑提问的稳定幂等键，格式为 {@code 调用方:操作:稳定标识}，例如
 *                       {@code web:chat-send:550e8400-e29b-41d4-a716-446655440000}。
 *                       SSE 断开、网络超时后重发同一提问时必须复用该值；新提问必须使用新值。
 */
public record SendMessageDTO(
        @NotBlank @Pattern(regexp = "[a-z][a-z0-9-]{0,31}:[a-z][a-z0-9-]{0,63}:[A-Za-z0-9-]{1,64}")
        @Size(max = 128) String idempotencyKey,
        @NotBlank @Size(max = 6000) String question,
        @Size(max = 500) String pageContext) {
}
