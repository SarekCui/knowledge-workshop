export type AgentMessageRole = 'user' | 'assistant';
export type AgentMessageStatus = 'streaming' | 'completed' | 'failed';

export interface AgentMessage {
  id: string;
  role: AgentMessageRole;
  content: string;
  status?: AgentMessageStatus;
  /** 从发问到回答结束（或失败）的耗时，毫秒；结束后固化到消息上。 */
  elapsedMs?: number;
}

export interface AgentChatSnapshot {
  conversationId?: string;
  messages: AgentMessage[];
  status: 'idle' | 'creating' | 'streaming' | 'error';
  error: string;
  runId?: string;
  /** 当前这一轮问答的开始时间戳，用于驱动思考计时。 */
  runStartedAt?: number;
  /** 面板视图：聊天或历史会话列表。 */
  view: 'chat' | 'history';
  /** 历史会话摘要列表（按最近更新倒序）。 */
  conversations: ConversationSummary[];
  loadingHistory: boolean;
}

export interface ConversationSummary {
  id: string;
  title: string;
  updatedAt: string;
}

export interface HistoryMessage {
  id: string;
  role: string;
  content: string;
  createdAt: string;
}

export interface AgentStreamEvent {
  event: string;
  data: unknown;
}

export interface AgentChatTransport {
  createConversation(title: string): Promise<{ id: string }>;
  listConversations(): Promise<ConversationSummary[]>;
  loadMessages(conversationId: string): Promise<HistoryMessage[]>;
  deleteConversation(conversationId: string): Promise<void>;
  stream(conversationId: string, request: { clientRequestId: string; question: string; pageContext: string },
    signal: AbortSignal, onEvent: (event: AgentStreamEvent) => void): Promise<void>;
}

const initialSnapshot: AgentChatSnapshot = { messages: [], status: 'idle', error: '', view: 'chat', conversations: [], loadingHistory: false };

export class AgentChatStore {
  private snapshot = initialSnapshot;
  private listeners = new Set<() => void>();
  private controller?: AbortController;

  constructor(private transport: AgentChatTransport,
    private uuid: () => string = () => crypto.randomUUID()) {}

  getSnapshot = () => this.snapshot;

  subscribe = (listener: () => void) => {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  };

  ask = async (question: string, pageContext: string) => {
    const content = question.trim();
    if (!content || this.snapshot.status === 'creating' || this.snapshot.status === 'streaming') return;
    const requestId = this.uuid();
    const startedAt = Date.now();
    const userMessage: AgentMessage = { id: requestId, role: 'user', content };
    const assistantMessage: AgentMessage = { id: `${requestId}:answer`, role: 'assistant', content: '', status: 'streaming' };
    this.publish({ messages: [...this.snapshot.messages, userMessage, assistantMessage], status: 'creating', error: '', runId: undefined, runStartedAt: startedAt });
    try {
      let conversationId = this.snapshot.conversationId;
      if (!conversationId) {
        const created = await this.transport.createConversation(`问答 · ${content.slice(0, 40)}`);
        conversationId = created.id;
        this.publish({
          conversations: [{ id: created.id, title: `问答 · ${content.slice(0, 40)}`, updatedAt: new Date().toISOString() },
            ...this.snapshot.conversations],
        });
      }
      const controller = new AbortController();
      this.controller = controller;
      this.publish({ conversationId, status: 'streaming' });
      await this.transport.stream(conversationId, { clientRequestId: requestId, question: content, pageContext }, controller.signal,
        event => this.consumeEvent(assistantMessage.id, event));
      if (this.snapshot.status !== 'error') {
        this.finalizeAssistant(assistantMessage.id, { status: 'completed' }, startedAt);
        this.publish({ status: 'idle', runStartedAt: undefined });
      }
    } catch (error) {
      if (error instanceof DOMException && error.name === 'AbortError') return;
      this.finalizeAssistant(assistantMessage.id, { status: 'failed', content: '小智暂时没有完成回答。' }, startedAt);
      this.publish({ status: 'error', error: error instanceof Error ? error.message : '小智服务暂时不可用', runStartedAt: undefined });
    } finally {
      this.controller = undefined;
    }
  };

  /** 删除历史会话：从列表移除；若删的是当前打开的会话，回到空聊天。 */
  deleteConversation = async (conversationId: string) => {
    if (this.snapshot.status === 'creating' || this.snapshot.status === 'streaming') return;
    try {
      await this.transport.deleteConversation(conversationId);
      const conversations = this.snapshot.conversations.filter(item => item.id !== conversationId);
      const change: Partial<AgentChatSnapshot> = { conversations };
      if (this.snapshot.conversationId === conversationId) {
        change.messages = [];
        change.conversationId = undefined;
        change.runId = undefined;
      }
      this.publish(change);
    } catch (error) {
      this.publish({ error: error instanceof Error ? error.message : '删除会话失败' });
    }
  };

  clear = () => {
    if (this.snapshot.status === 'creating' || this.snapshot.status === 'streaming') return;
    this.publish({ messages: [], conversationId: undefined, runId: undefined, status: 'idle', error: '', runStartedAt: undefined, view: 'chat' });
  };

  /** 打开历史会话列表：拉取当前用户最近的会话。 */
  openHistory = async () => {
    if (this.snapshot.status === 'creating' || this.snapshot.status === 'streaming') return;
    this.publish({ view: 'history', loadingHistory: true, error: '' });
    try {
      const conversations = await this.transport.listConversations();
      this.publish({ conversations, loadingHistory: false });
    } catch (error) {
      this.publish({ loadingHistory: false, view: 'chat', error: error instanceof Error ? error.message : '历史会话加载失败' });
    }
  };

  closeHistory = () => {
    if (this.snapshot.status === 'creating' || this.snapshot.status === 'streaming') return;
    this.publish({ view: 'chat' });
  };

  /** 选择一个历史会话：加载它的消息并回到聊天视图继续聊。 */
  selectConversation = async (conversationId: string) => {
    if (this.snapshot.status === 'creating' || this.snapshot.status === 'streaming') return;
    this.publish({ loadingHistory: true, error: '' });
    try {
      const history = await this.transport.loadMessages(conversationId);
      const messages: AgentMessage[] = history.map(item => ({
        id: item.id,
        role: item.role === 'USER' ? 'user' : 'assistant',
        content: item.content,
        status: 'completed',
      }));
      this.publish({ messages, conversationId, runId: undefined, status: 'idle', error: '', loadingHistory: false, view: 'chat' });
    } catch (error) {
      this.publish({ loadingHistory: false, error: error instanceof Error ? error.message : '历史消息加载失败' });
    }
  };

  private consumeEvent(assistantMessageId: string, event: AgentStreamEvent) {
    if (event.event === 'run.started' && isObject(event.data) && typeof event.data.runId === 'string') {
      this.publish({ runId: event.data.runId });
      return;
    }
    if (event.event === 'answer.delta' && isObject(event.data) && typeof event.data.text === 'string') {
      const message = this.snapshot.messages.find(item => item.id === assistantMessageId);
      if (message) this.markAssistant(assistantMessageId, { content: message.content + event.data.text });
      return;
    }
    if (event.event === 'answer.completed') {
      this.finalizeAssistant(assistantMessageId, { status: 'completed' });
      this.publish({ status: 'idle', runStartedAt: undefined });
      return;
    }
    if (event.event === 'answer.failed') {
      this.finalizeAssistant(assistantMessageId, { status: 'failed', content: '小智暂时没有完成回答。' });
      this.publish({ status: 'error', error: '模型服务暂时不可用，请稍后再试。', runStartedAt: undefined });
    }
  }

  private finalizeAssistant(id: string, patch: Partial<AgentMessage>, startedAt?: number) {
    const elapsedMs = startedAt ?? this.snapshot.runStartedAt;
    const withElapsed = elapsedMs === undefined ? patch : { ...patch, elapsedMs: Math.max(0, Date.now() - elapsedMs) };
    this.markAssistant(id, withElapsed);
  }

  private markAssistant(id: string, patch: Partial<AgentMessage>) {
    this.publish({ messages: this.snapshot.messages.map(message => message.id === id ? { ...message, ...patch } : message) });
  }

  private publish(change: Partial<AgentChatSnapshot>) {
    this.snapshot = { ...this.snapshot, ...change };
    this.listeners.forEach(listener => listener());
  }
}

function isObject(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null;
}
