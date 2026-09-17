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
  parentCommentId: string | null;
  content: string;
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
  busyAction: 'like' | 'favorite' | 'comment' | 'delete' | null;
  engagement?: NoteEngagement;
  comments: NoteComment[];
  page: number;
  total: number;
  input: string;
  parentCommentId: string | null;
  commentLocked: boolean;
}

export class NoteInteractionStore {
  private state: Snapshot = {
    status: 'idle', error: '', busyAction: null, comments: [], page: 1, total: 0,
    input: '', parentCommentId: null, commentLocked: false,
  };
  private listeners = new Set<() => void>();
  private generation = 0;
  private commentRequestId: string | null = null;

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
    if (!this.state.commentLocked && this.state.busyAction !== 'comment') this.publish({ input: input.slice(0, 1000) });
  };
  replyTo = (commentId: string | null) => {
    if (!this.state.commentLocked && this.state.busyAction !== 'comment') this.publish({ parentCommentId: commentId });
  };

  submitComment = () => this.perform('comment', async () => {
    const content = this.state.input.trim();
    if (!content) throw new Error('评论内容不能为空');
    this.commentRequestId ??= this.uuid();
    try {
      const comment = await this.transport<NoteComment>(`${this.publicPath()}/comments`, {
        clientRequestId: this.commentRequestId,
        parentCommentId: this.state.parentCommentId,
        content,
      });
      this.commentRequestId = null;
      const engagement = this.state.engagement;
      const nextEngagement = engagement ? { ...engagement, commentCount: engagement.commentCount + 1 } : engagement;
      this.publish({ comments: [...this.state.comments.filter(item => item.id !== comment.id), comment],
        total: this.state.total + (this.state.comments.some(item => item.id === comment.id) ? 0 : 1),
        engagement: nextEngagement,
        input: '', parentCommentId: null, commentLocked: false });
      if (nextEngagement) synchronizeNoteEngagement(this.syncScope, this.noteId, nextEngagement);
    } catch (error) {
      if (error instanceof ApiError && error.status === 400) {
        this.commentRequestId = null;
        this.publish({ commentLocked: false });
      } else {
        this.publish({ commentLocked: true });
      }
      throw error;
    }
  });

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
  private async perform(action: NonNullable<Snapshot['busyAction']>, operation: () => Promise<void>) {
    if (this.state.busyAction || this.state.status === 'loading') return;
    this.publish({ busyAction: action, error: '' });
    try { await operation(); }
    catch (error) { this.publish({ error: this.message(error) }); }
    finally { this.publish({ busyAction: null }); }
  }
  private message(error: unknown) { return error instanceof Error ? error.message : '请求失败，请重试'; }
}
