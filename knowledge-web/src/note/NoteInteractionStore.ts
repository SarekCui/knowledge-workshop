import { ApiError } from '../api';
import type { NoteTransport } from './NoteStore';
import { synchronizeNoteEngagement } from './NoteEngagementSync';

export interface NoteEngagement {
  likeCount: number;
  favoriteCount: number;
  commentCount: number;
  liked: boolean;
  favorited: boolean;
}

export interface NoteComment {
  id: string;
  noteId: string;
  authorId: string;
  authorType: 'USER' | 'AGENT';
  parentCommentId: string | null;
  sourceCommentId?: string | null;
  content: string;
  likeCount: number;
  liked: boolean;
  owned: boolean;
  version: number;
  createdAt: string;
  updatedAt: string;
}

interface CommentPage {
  items: NoteComment[];
  pageNo: number;
  pageSize: number;
  total: number;
}

interface Snapshot {
  status: 'idle' | 'loading' | 'success' | 'error';
  error: string;
  busyAction: 'like' | 'favorite' | 'comment' | 'comment-like' | 'delete' | null;
  engagement?: NoteEngagement;
  comments: NoteComment[];
  page: number;
  total: number;
  input: string;
  replyInput: string;
  parentCommentId: string | null;
  commentLocked: boolean;
}

export class NoteInteractionStore {
  private state: Snapshot = {
    status: 'idle', error: '', busyAction: null, comments: [], page: 1, total: 0,
    input: '', replyInput: '', parentCommentId: null, commentLocked: false,
  };
  private listeners = new Set<() => void>();
  private generation = 0;
  private commentRequest: { id: string; parentCommentId: string | null; content: string } | null = null;

  /** 笔记作者 ID：评论者为笔记作者时展示「作者」徽章。 */
  noteAuthorId = '';

  constructor(private noteId: string, private transport: NoteTransport,
    private uuid: () => string = () => crypto.randomUUID(), private syncScope: NoteTransport = transport) {}

  getSnapshot = () => this.state;
  subscribe = (listener: () => void) => { this.listeners.add(listener); return () => { this.listeners.delete(listener); }; };
  private publish(change: Partial<Snapshot>) {
    this.state = { ...this.state, ...change };
    this.listeners.forEach(listener => listener());
  }
  cancel = () => { ++this.generation; };

  load = async () => {
    const generation = ++this.generation;
    this.publish({ status: 'loading', error: '' });
    try {
      const [engagement, page] = await Promise.all([
        this.transport<NoteEngagement>(this.engagementPath()),
        this.readComments(1),
      ]);
      if (generation === this.generation) {
        synchronizeNoteEngagement(this.syncScope, this.noteId, engagement);
        this.publish({ engagement, comments: page.items, page: page.pageNo, total: page.total, status: 'success' });
      }
    } catch (error) {
      if (generation === this.generation) this.publish({ status: 'error', error: this.message(error) });
    }
  };

  loadComments = async (page: number) => {
    const generation = ++this.generation;
    this.publish({ status: 'loading', error: '' });
    try {
      const result = await this.readComments(page);
      if (generation === this.generation) this.publish({ comments: result.items, page: result.pageNo,
        total: result.total, status: 'success' });
    } catch (error) {
      if (generation === this.generation) this.publish({ status: 'error', error: this.message(error) });
    }
  };

  toggleLike = () => this.toggle('like');
  toggleFavorite = () => this.toggle('favorite');
  toggleCommentLike = (commentId: string) => this.perform('comment-like', async () => {
    const previous = this.state.comments.find(comment => comment.id === commentId);
    if (!previous) return;
    const optimistic = { ...previous, liked: !previous.liked,
      likeCount: Math.max(0, previous.likeCount + (previous.liked ? -1 : 1)) };
    this.publish({ comments: this.state.comments.map(comment => comment.id === commentId ? optimistic : comment) });
    try {
      const comment = await this.transport<NoteComment>(`/api/learning/notes/comments/${encodeURIComponent(commentId)}/likes`,
        undefined, { method: previous.liked ? 'DELETE' : 'PUT' });
      this.publish({ comments: this.state.comments.map(item => item.id === commentId ? comment : item) });
    } catch (error) {
      this.publish({ comments: this.state.comments.map(item => item.id === commentId ? previous : item) });
      throw error;
    }
  });
  private toggle(action: 'like' | 'favorite') {
    return this.perform(action, async () => {
      const previous = this.state.engagement;
      if (!previous) return;
      const flag = action === 'like' ? 'liked' : 'favorited';
      const count = action === 'like' ? 'likeCount' : 'favoriteCount';
      const optimistic = { ...previous, [flag]: !previous[flag],
        [count]: Math.max(0, previous[count] + (previous[flag] ? -1 : 1)) };
      this.publish({ engagement: optimistic });
      synchronizeNoteEngagement(this.syncScope, this.noteId, optimistic);
      try {
        const engagement = await this.transport<NoteEngagement>(`${this.publicPath()}/${action === 'like' ? 'likes' : 'favorites'}`,
          undefined, { method: previous[flag] ? 'DELETE' : 'PUT' });
        this.publish({ engagement });
        synchronizeNoteEngagement(this.syncScope, this.noteId, engagement);
      } catch (error) {
        this.publish({ engagement: previous });
        synchronizeNoteEngagement(this.syncScope, this.noteId, previous);
        throw error;
      }
    });
  }

  setInput = (input: string) => {
    if (this.state.commentLocked || this.state.busyAction === 'comment') return;
    const value = input.slice(0, 1000);
    this.publish(this.state.parentCommentId ? { replyInput: value } : { input: value });
  };
  setRootInput = (input: string) => {
    if (!this.state.commentLocked && this.state.busyAction !== 'comment') this.publish({ input: input.slice(0, 1000) });
  };
  replyTo = (commentId: string | null) => {
    if (!this.state.commentLocked && this.state.busyAction !== 'comment') {
      this.publish({ parentCommentId: commentId, replyInput: '' });
    }
  };

  submitRootComment = () => this.submitCommentFor(null);
  submitComment = () => this.submitCommentFor(this.state.parentCommentId);
  private submitCommentFor(parentCommentId: string | null) {
    return this.perform('comment', async () => {
    const content = (parentCommentId ? this.state.replyInput : this.state.input).trim();
    if (!content) throw new Error('评论内容不能为空');
    this.commentRequest ??= { id: this.uuid(), parentCommentId, content };
    try {
      const comment = await this.transport<NoteComment>(`${this.publicPath()}/comments`, {
        idempotencyKey: `web:comment-create:${this.commentRequest.id}`,
        parentCommentId: this.commentRequest.parentCommentId,
        content: this.commentRequest.content,
      });
      this.commentRequest = null;
      const engagement = this.state.engagement;
      const nextEngagement = engagement ? { ...engagement, commentCount: engagement.commentCount + 1 } : engagement;
      const clearedDraft = parentCommentId
        ? { replyInput: '', parentCommentId: null }
        : { input: '' };
      this.publish({ comments: [...this.state.comments.filter(item => item.id !== comment.id), comment],
        total: this.state.total + (this.state.comments.some(item => item.id === comment.id) ? 0 : 1),
        engagement: nextEngagement,
        ...clearedDraft, commentLocked: false });
      if (nextEngagement) synchronizeNoteEngagement(this.syncScope, this.noteId, nextEngagement);
      if (/@小智/.test(content)) void this.pollForAgentReply(comment.id);
    } catch (error) {
      if (error instanceof ApiError && error.status === 400) {
        this.commentRequest = null;
        this.publish({ commentLocked: false });
      } else {
        this.publish({ commentLocked: true });
      }
      throw error;
    }
    });
  }

  deleteComment = (commentId: string) => this.perform('delete', async () => {
    const comment = this.state.comments.find(item => item.id === commentId);
    if (!comment?.owned) return;
    await this.transport(`/api/learning/notes/comments/${encodeURIComponent(commentId)}?version=${comment.version}`,
      undefined, { method: 'DELETE' });
    const engagement = this.state.engagement;
    const nextEngagement = engagement ? { ...engagement,
      commentCount: Math.max(0, engagement.commentCount - 1) } : engagement;
    this.publish({ comments: this.state.comments.filter(item => item.id !== commentId),
      total: Math.max(0, this.state.total - 1),
      engagement: nextEngagement });
    if (nextEngagement) synchronizeNoteEngagement(this.syncScope, this.noteId, nextEngagement);
  });

  private engagementPath() { return `${this.publicPath()}/engagement`; }
  private publicPath() { return `/api/learning/notes/public/${encodeURIComponent(this.noteId)}`; }
  private readComments(page: number) {
    return this.transport<CommentPage>(`${this.publicPath()}/comments?pageNo=${page}&pageSize=50`);
  }

  /** @小智 后小智回复是异步的，轻量轮询评论列表直到回复出现或超时。 */
  private async pollForAgentReply(sourceCommentId: string) {
    const placeholderId = 'pending-' + sourceCommentId;
    const placeholder: NoteComment = {
      id: placeholderId, noteId: this.noteId, authorId: 'agent', authorType: 'AGENT',
      content: '小智正在思考…', parentCommentId: sourceCommentId, sourceCommentId,
      likeCount: 0, liked: false, createdAt: new Date().toISOString(), updatedAt: new Date().toISOString(), version: 0, owned: false,
    };
    this.publish({ comments: [...this.state.comments, placeholder] });
    const page = this.state.page;
    for (let attempt = 0; attempt < 12; attempt += 1) {
      await new Promise(resolve => setTimeout(resolve, 2500));
      try {
        const result = await this.readComments(page);
        const replied = result.items.find(item => item.sourceCommentId === sourceCommentId && item.authorType === 'AGENT');
        if (replied) {
          // 占位替换成真实回复，无闪烁
          this.publish({ comments: [...result.items.filter(item => item.id !== placeholderId && item.id !== replied.id), replied], total: result.total });
          return;
        }
        // 还没回复，保留占位
        this.publish({ comments: [...result.items.filter(item => item.id !== placeholderId), placeholder], total: result.total });
      } catch { /* 轮询失败不打断用户，下次手动刷新即可 */ }
    }
  }
  private async perform(action: NonNullable<Snapshot['busyAction']>, operation: () => Promise<void>) {
    if (this.state.busyAction || this.state.status === 'loading') return;
    this.publish({ busyAction: action, error: '' });
    try { await operation(); }
    catch (error) { this.publish({ error: this.message(error) }); }
    finally { this.publish({ busyAction: null }); }
  }
  private message(error: unknown) { return error instanceof Error ? error.message : '请求失败，请重试'; }
}
