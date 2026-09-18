import { describe, expect, it, vi } from 'vitest';
import { AgentChatStore, type AgentChatTransport } from './AgentChatStore';
import { readSse } from './agentSse';

describe('AgentChatStore', () => {
  it('创建会话、拼接流式回答并保存运行标识', async () => {
    let emit: (event: { event: string; data: unknown }) => void = () => {};
    const transport: AgentChatTransport = {
      createConversation: vi.fn().mockResolvedValue({ id: 'conversation-1' }),
      listConversations: vi.fn().mockResolvedValue([]),
      loadMessages: vi.fn().mockResolvedValue([]),
      deleteConversation: vi.fn().mockResolvedValue(undefined),
      stream: vi.fn().mockImplementation(async (_id, _request, _signal, onEvent) => {
        emit = onEvent;
        emit({ event: 'run.started', data: { runId: 'run-1' } });
        emit({ event: 'answer.delta', data: { text: 'Redis ' } });
        emit({ event: 'answer.delta', data: { text: '用于缓存。' } });
        emit({ event: 'answer.completed', data: { inputTokens: 1, outputTokens: 2 } });
      }),
    };
    const store = new AgentChatStore(transport, () => 'request-1');

    await store.ask('Redis 是什么？', '当前页面：/courses');

    expect(transport.createConversation).toHaveBeenCalledWith('问答 · Redis 是什么？');
    expect(store.getSnapshot()).toMatchObject({ conversationId: 'conversation-1', runId: 'run-1', status: 'idle' });
    expect(store.getSnapshot().runStartedAt).toBeUndefined();
    expect(store.getSnapshot().messages).toEqual([
      { id: 'request-1', role: 'user', content: 'Redis 是什么？' },
      { id: 'request-1:answer', role: 'assistant', content: 'Redis 用于缓存。', status: 'completed', elapsedMs: expect.any(Number) },
    ]);
    expect(store.getSnapshot().messages[1]?.elapsedMs).toBeGreaterThanOrEqual(0);
  });

  it('模型失败时保留用户问题并展示可见错误', async () => {
    const transport: AgentChatTransport = {
      createConversation: vi.fn().mockResolvedValue({ id: 'conversation-1' }),
      listConversations: vi.fn().mockResolvedValue([]),
      loadMessages: vi.fn().mockResolvedValue([]),
      deleteConversation: vi.fn().mockResolvedValue(undefined),
      stream: vi.fn().mockImplementation(async (_id, _request, _signal, onEvent) => {
        onEvent({ event: 'answer.failed', data: { code: 'MODEL_UNAVAILABLE' } });
      }),
    };
    const store = new AgentChatStore(transport, () => 'request-2');

    await store.ask('解释缓存穿透', '当前页面：/notes');

    expect(store.getSnapshot().status).toBe('error');
    expect(store.getSnapshot().error).toContain('模型服务');
    expect(store.getSnapshot().messages[0]?.content).toBe('解释缓存穿透');
    expect(store.getSnapshot().messages[1]?.status).toBe('failed');
    expect(store.getSnapshot().messages[1]?.elapsedMs).toEqual(expect.any(Number));
  });
});

describe('readSse', () => {
  it('可跨网络分帧重组 SSE 事件', async () => {
    const encoder = new TextEncoder();
    const body = new ReadableStream<Uint8Array>({
      start(controller) {
        controller.enqueue(encoder.encode('event: answer.delta\ndata: {"text":"Redis"'));
        controller.enqueue(encoder.encode('}\n\nevent: answer.completed\ndata: {"outputTokens":2}\n\n'));
        controller.close();
      },
    });
    const events: { event: string; data: unknown }[] = [];

    await readSse(body, event => events.push(event));

    expect(events).toEqual([
      { event: 'answer.delta', data: { text: 'Redis' } },
      { event: 'answer.completed', data: { outputTokens: 2 } },
    ]);
  });
});
