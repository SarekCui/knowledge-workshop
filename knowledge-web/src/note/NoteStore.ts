import { ApiError, type RequestOptions } from '../api';

export interface Note {
  id: string; courseId: string | null; chapterId: string | null; title: string; content: string;
  videoPositionMs: number | null; version: number; createdAt: string; updatedAt: string;
  authorId?: string; status?: 'DRAFT' | 'PRIVATE' | 'PUBLIC'; publishedAt?: string | null;
  likeCount?: number; favoriteCount?: number; commentCount?: number;
  liked?: boolean; favorited?: boolean;
  tags?: string[];
}
export interface NoteDraft {
  mode: 'create' | 'edit' | 'rename'; title: string; content: string;
  tags: string[];
  original?: Note; idempotencyKey?: string; locked: boolean; conflict: boolean;
  courseId?: string | null;
}
interface Snapshot {
  notes: Note[]; status: 'idle' | 'loading' | 'success' | 'error'; busy: boolean;
  draft?: NoteDraft; error: string; notice: string;
  searchInput: string; keyword: string;
  page: number; total: number; courses: { id: string; title: string }[]; coursesError: string;
}
interface NotePage { items: Note[]; total: number; pageNo: number }
export interface NoteImageUpload { id: string; markdownUrl: string; width: number; height: number }
export type NoteTransport = <T>(path: string, body?: unknown, options?: RequestOptions) => Promise<T>;

export class NoteStore {
  private snapshot: Snapshot = { notes: [], status: 'idle', busy: false, error: '', notice: '', searchInput: '', keyword: '', page: 1, total: 0, courses: [], coursesError: '' };
  private listeners = new Set<() => void>();
  private generation = 0;
  private lifecycle = 0;
  constructor(private courseId: string | null, private transport: NoteTransport,
    private uuid: () => string = () => crypto.randomUUID(), readonly paginated = false) {}
  get allowCourseSelection() { return this.courseId === null; }
  loadCourses = async () => {
    const lifecycle = this.lifecycle;
    try {
      const courses = await this.transport<Snapshot['courses']>('/api/learning/courses');
      if (lifecycle === this.lifecycle) this.publish({ courses, coursesError: '' });
    } catch (error) {
      if (lifecycle === this.lifecycle) this.publish({ coursesError: `课程选项加载失败；仍可创建独立笔记。${this.message(error)}` });
    }
  };
  getSnapshot = () => this.snapshot;
  subscribe = (listener: () => void) => { this.listeners.add(listener); return () => { this.listeners.delete(listener); }; };
  private publish(change: Partial<Snapshot>) {
    this.snapshot = { ...this.snapshot, ...change }; this.listeners.forEach(listener => listener());
  }
  cancelPending = () => { ++this.generation; ++this.lifecycle; };
  setSearchInput = (value: string) => {
    if (!this.snapshot.busy && !this.snapshot.draft) this.publish({ searchInput: value.slice(0, 100) });
  };
  search = () => this.load(this.snapshot.searchInput.trim(), 1);
  clearSearch = () => {
    if (this.snapshot.busy || this.snapshot.draft) return Promise.resolve();
    this.publish({ searchInput: '' });
    return this.load('', 1);
  };
  private listPath() {
    const query = new URLSearchParams();
    if (this.courseId) query.set('courseId', this.courseId);
    if (this.paginated) { query.set('pageNo', String(this.snapshot.page)); query.set('pageSize', '20'); }
    else query.set('limit', '100');
    if (this.snapshot.keyword) query.set('keyword', this.snapshot.keyword);
    return `/api/learning/notes${this.paginated ? '/mine' : ''}?${query}`;
  }
  private async readList() {
    if (this.paginated) {
      const page = await this.transport<NotePage>(this.listPath());
      return { notes: page.items, total: page.total, page: page.pageNo };
    }
    return { notes: await this.transport<Note[]>(this.listPath()) };
  }
  load = async (keyword = this.snapshot.keyword, page = this.snapshot.page) => {
    if (this.snapshot.busy || this.snapshot.draft) return;
    const generation = ++this.generation;
    this.publish({ status: 'loading', error: '', keyword, page });
    try {
      const result = await this.readList();
      if (generation === this.generation) this.publish({ ...result, status: 'success' });
    } catch (error) {
      if (generation === this.generation) this.publish({ status: 'error', error: this.message(error) });
    }
  };
  private async refreshSearchAfterWrite() {
    if (!this.snapshot.keyword && !this.paginated) return;
    const generation = this.generation;
    this.publish({ status: 'loading' });
    try {
      const result = await this.readList();
      if (generation === this.generation) this.publish({ ...result, status: 'success' });
    } catch (error) {
      if (generation === this.generation) this.publish({ status: 'error', error: `操作已成功，但搜索结果刷新失败：${this.message(error)}` });
    }
  }
  create = () => {
    if (this.snapshot.busy || this.snapshot.draft || this.snapshot.status === 'loading') return;
    this.publish({ draft: { mode: 'create', title: '', content: '', tags: [], courseId: this.courseId, idempotencyKey: `web:note-create:${this.uuid()}`, locked: false, conflict: false }, error: '', notice: '' });
  };
  open = (id: string, mode: 'edit' | 'rename' = 'edit') => this.perform(async () => {
    const note = await this.transport<Note>(`/api/learning/notes/${encodeURIComponent(id)}`);
    this.publish({ draft: { mode, original: note, title: note.title, content: note.content,
      tags: note.tags ?? [], locked: false, conflict: false } });
  });
  edit = (field: 'title' | 'content', value: string) => {
    const draft = this.snapshot.draft;
    if (!draft || draft.locked || this.snapshot.busy) return;
    this.publish({ draft: { ...draft, [field]: value } });
  };
  editTags = (values: string[]) => {
    const draft = this.snapshot.draft;
    if (!draft || draft.locked || this.snapshot.busy || draft.mode === 'rename') return;
    const unique = new Map<string, string>();
    values.forEach(value => {
      const display = value.normalize('NFKC').trim().replace(/\s+/g, ' ');
      if (display) unique.set(display.toLocaleLowerCase(), display);
    });
    this.publish({ draft: { ...draft, tags: [...unique.values()].slice(0, 5) } });
  };
  selectCourse = (courseId: string | null) => {
    const draft = this.snapshot.draft;
    if (draft?.mode === 'create' && !draft.locked && !this.snapshot.busy && this.allowCourseSelection) {
      this.publish({ draft: { ...draft, courseId } });
    }
  };
  uploadImage = async (file: File): Promise<NoteImageUpload | undefined> => {
    const draft = this.snapshot.draft;
    if (!draft || draft.mode === 'rename' || draft.locked || this.snapshot.busy) return undefined;
    if (!['image/jpeg', 'image/png'].includes(file.type)) {
      this.publish({ error: '仅支持 JPEG 或 PNG 图片' }); return undefined;
    }
    if (file.size > 5 * 1024 * 1024) {
      this.publish({ error: 'Note 图片不能超过5MB' }); return undefined;
    }
    const lifecycle = this.lifecycle;
    this.publish({ busy: true, error: '', notice: '' });
    const form = new FormData(); form.append('file', file);
    try {
      const image = await this.transport<NoteImageUpload>('/api/learning/note-images', form);
      return lifecycle === this.lifecycle ? image : undefined;
    } catch (error) {
      if (lifecycle === this.lifecycle) this.publish({ error: this.message(error) });
      return undefined;
    } finally {
      if (lifecycle === this.lifecycle) this.publish({ busy: false });
    }
  };
  changeStatus = (id: string, status: NonNullable<Note['status']>) => this.perform(async () => {
    const original = this.snapshot.notes.find(note => note.id === id);
    if (!original) return;
    const note = await this.transport<Note>(`/api/learning/notes/${encodeURIComponent(id)}/status`,
      { status, version: original.version }, { method: 'PATCH' });
    this.publish({ notes: this.snapshot.notes.map(item => item.id === id ? note : item), notice: status === 'PUBLIC' ? '已公开发布' : '已撤回为非公开笔记' });
    await this.refreshSearchAfterWrite();
  });
  close = () => { if (!this.snapshot.busy) this.publish({ draft: undefined, error: '' }); };
  save = () => this.perform(async () => {
    const draft = this.snapshot.draft;
    if (!draft || draft.conflict) return;
    if (!draft.title.trim() || draft.title.length > 100 || draft.content.length > 20000) {
      throw new Error('标题不能为空且最多100个字符；正文最多20000个字符');
    }
    if (draft.tags.some(tag => tag.length > 20)) throw new Error('每个技术标签最多20个字符');
    let note: Note;
    try {
      if (draft.mode === 'create') {
        this.publish({ draft: { ...draft, locked: true } });
        note = await this.transport<Note>('/api/learning/notes', {
          idempotencyKey: draft.idempotencyKey, courseId: draft.courseId ?? this.courseId,
          title: draft.title, content: draft.content, chapterId: null, videoPositionMs: null, tags: draft.tags,
        });
      } else {
        const original = draft.original!;
        note = await this.transport<Note>(`/api/learning/notes/${encodeURIComponent(original.id)}${draft.mode === 'rename' ? '/title' : ''}`,
          draft.mode === 'rename' ? { title: draft.title, version: original.version }
            : { title: draft.title, content: draft.content, videoPositionMs: original.videoPositionMs,
              version: original.version, tags: draft.tags },
          { method: draft.mode === 'rename' ? 'PATCH' : 'PUT' });
      }
    } catch (error) {
      if (draft.mode !== 'create' && error instanceof ApiError && error.status === 409) {
        this.publish({ draft: { ...draft, conflict: true } });
        throw new Error('Note 已被其他设备修改。草稿已保留，请复制需要的内容，或放弃草稿后重新打开最新版本。');
      }
      if (draft.mode === 'create' && !draft.locked && error instanceof ApiError && error.status === 400) {
        this.publish({ draft: { ...draft, locked: false, idempotencyKey: `web:note-create:${this.uuid()}` } });
        throw error;
      }
      if (draft.mode === 'create') throw new Error(`创建结果未确认，已锁定本次内容；重试会复用原幂等键。${this.message(error)}`);
      throw error;
    }
    this.publish({ notes: [note, ...this.snapshot.notes.filter(item => item.id !== note.id)].slice(0, 100),
      draft: undefined, status: 'success', notice: draft.mode === 'create' ? 'Note 创建成功' : 'Note 保存成功' });
    await this.refreshSearchAfterWrite();
  });
  remove = (id: string) => this.perform(async () => {
    const note = this.snapshot.notes.find(item => item.id === id);
    if (!note) return;
    await this.transport(`/api/learning/notes/${encodeURIComponent(id)}?version=${note.version}`, undefined, { method: 'DELETE' });
    this.publish({ notes: this.snapshot.notes.filter(item => item.id !== id), notice: 'Note 已删除' });
    await this.refreshSearchAfterWrite();
  });
  private async perform(action: () => Promise<void>) {
    if (this.snapshot.busy || this.snapshot.status === 'loading') return;
    ++this.generation; this.publish({ busy: true, error: '', notice: '' });
    try { await action(); }
    catch (error) { this.publish({ error: this.message(error) }); }
    finally { this.publish({ busy: false }); }
  }
  private message(error: unknown) { return error instanceof Error ? error.message : '请求失败，请重试'; }
}
