import { ApiError, type RequestOptions } from '../api';
import type { AgentChatTransport, AgentStreamEvent } from './AgentChatStore';

type AuthRequest = <T>(path: string, body?: unknown, options?: RequestOptions) => Promise<T>;
type AuthStreamRequest = (path: string, body: unknown, signal: AbortSignal) => Promise<Response>;

export function createAgentChatTransport(request: AuthRequest, streamRequest: AuthStreamRequest): AgentChatTransport {
  return {
    createConversation: title => request('/api/agent/conversations', { title }),
    listConversations: () => request('/api/agent/conversations', undefined, { method: 'GET' }),
    loadMessages: conversationId =>
      request(`/api/agent/conversations/${encodeURIComponent(conversationId)}/messages`, undefined, { method: 'GET' }),
    deleteConversation: conversationId =>
      request(`/api/agent/conversations/${encodeURIComponent(conversationId)}`, undefined, { method: 'DELETE' }),
    async stream(conversationId, payload, signal, onEvent) {
      const response = await streamRequest(`/api/agent/conversations/${encodeURIComponent(conversationId)}/messages/stream`, payload, signal);
      if (!response.ok) throw await responseError(response);
      if (!response.body) throw new Error('小智未返回流式响应');
      await readSse(response.body, onEvent);
    },
  };
}

export async function readSse(body: ReadableStream<Uint8Array>, onEvent: (event: AgentStreamEvent) => void) {
  const reader = body.getReader();
  const decoder = new TextDecoder();
  let buffer = '';
  let event = 'message';
  let data: string[] = [];
  const dispatch = () => {
    if (data.length) {
      const raw = data.join('\n');
      let parsed: unknown = raw;
      try { parsed = JSON.parse(raw); } catch { /* A valid SSE payload can also be plain text. */ }
      onEvent({ event, data: parsed });
    }
    event = 'message';
    data = [];
  };
  try {
    while (true) {
      const result = await reader.read();
      buffer += decoder.decode(result.value, { stream: !result.done });
      let lineEnd: number;
      while ((lineEnd = buffer.indexOf('\n')) >= 0) {
        const line = buffer.slice(0, lineEnd).replace(/\r$/, '');
        buffer = buffer.slice(lineEnd + 1);
        if (!line) dispatch();
        else if (line.startsWith('event:')) event = line.slice(6).trim();
        else if (line.startsWith('data:')) data.push(line.slice(5).trimStart());
      }
      if (result.done) break;
    }
    if (buffer) {
      const line = buffer.replace(/\r$/, '');
      if (line.startsWith('event:')) event = line.slice(6).trim();
      if (line.startsWith('data:')) data.push(line.slice(5).trimStart());
    }
    dispatch();
  } finally {
    reader.releaseLock();
  }
}

async function responseError(response: Response) {
  const payload = await response.json().catch(() => null);
  return new ApiError(response.status, payload?.message ?? `小智请求失败（HTTP ${response.status}）`, payload?.requestId);
}
