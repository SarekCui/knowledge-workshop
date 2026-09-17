import type { Note, NoteTransport } from './NoteStore';
import { mergeSynchronizedEngagement, subscribeNoteEngagement } from './NoteEngagementSync';

interface Snapshot {
  loadingMore: boolean; moreError: string;
  status: 'idle' | 'loading' | 'success' | 'error'; error: string; notes: Note[]; note?: Note;
  input: string; keyword: string; tag: string; sort: 'LATEST' | 'HOT'; page: number; total: number;
}
export type NoteCollection = 'public' | 'liked' | 'favorites';
export class NoteBrowseStore {
  private state: Snapshot = { loadingMore: false, moreError: '', status: 'idle', error: '', notes: [], input: '', keyword: '', tag: '', sort: 'LATEST', page: 1, total: 0 };
  private listeners = new Set<() => void>();
  private generation = 0;
  private unsubscribeEngagement: () => void;
  constructor(private transport: NoteTransport, readonly noteId?: string, readonly courseId?: string,
    readonly collection: NoteCollection = 'public') {
    this.unsubscribeEngagement = subscribeNoteEngagement(transport, (changedId, engagement) => {
      const update = (note: Note) => note.id === changedId ? { ...note, ...engagement } : note;
      const notes = this.state.notes.map(update);
      const note = this.state.note ? update(this.state.note) : undefined;
      if (notes.some((item, index) => item !== this.state.notes[index]) || note !== this.state.note) {
        this.publish({ notes, note });
      }
    });
  }
  getSnapshot = () => this.state;
  subscribe = (listener: () => void) => { this.listeners.add(listener); return () => { this.listeners.delete(listener); }; };
  private publish(change: Partial<Snapshot>) { this.state = { ...this.state, ...change }; this.listeners.forEach(fn => fn()); }
  cancel = () => { ++this.generation; this.unsubscribeEngagement(); };
  setInput = (input: string) => this.publish({ input: input.slice(0, 100) });
  setSort = (sort: Snapshot['sort']) => { this.publish({ sort }); return this.load(1); };
  search = () => this.load(1, this.state.input.trim());
  clear = () => { this.publish({ input: '' }); return this.load(1, ''); };
  selectTag = (tag: string) => { this.publish({ tag }); return this.load(1); };
  clearTag = () => { this.publish({ tag: '' }); return this.load(1); };
  loadMore = () => {
    if (this.state.loadingMore || this.state.status !== 'success' || this.state.notes.length >= this.state.total) return;
    return this.load(this.state.page + 1, this.state.keyword, true);
  };
  load = async (page = this.state.page, keyword = this.state.keyword, append = false) => {
    const generation = ++this.generation;
    this.publish(append ? { loadingMore: true, moreError: '' } : { status: 'loading', loadingMore: false, moreError: '', error: '', notes: [], note: undefined, page, keyword });
    try {
      if (this.noteId) {
        const note = await this.transport<Note>(`/api/learning/notes/public/${encodeURIComponent(this.noteId)}`);
        if (generation === this.generation) this.publish({ note: mergeSynchronizedEngagement(this.transport, note), status: 'success' });
      } else {
        const query = new URLSearchParams({ pageNo: String(page), pageSize: '20' });
        if (this.collection === 'public') {
          if (keyword) query.set('keyword', keyword);
          if (this.state.tag) query.set('tag', this.state.tag);
          if (this.courseId) query.set('courseId', this.courseId);
          query.set('sort', this.state.sort);
        }
        const endpoint = this.collection === 'public' ? 'public' : this.collection;
        const result = await this.transport<{ items: Note[]; total: number; pageNo: number }>(`/api/learning/notes/${endpoint}?${query}`);
        const items = result.items.map(note => mergeSynchronizedEngagement(this.transport, note));
        if (generation === this.generation) this.publish({ notes: append
          ? [...new Map([...this.state.notes, ...items].map(note => [note.id, note])).values()] : items,
          loadingMore: false, total: result.total, page: result.pageNo, status: 'success' });
      }
    } catch (error) {
      if (generation === this.generation) {
        const message = error instanceof Error ? error.message : '笔记暂时不可用';
        this.publish(append ? { loadingMore: false, moreError: message } : { status: 'error', error: message });
      }
    }
  };
}
