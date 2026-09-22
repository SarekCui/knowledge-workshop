import { describe, expect, it, vi } from 'vitest';
import { ApiError, type RequestOptions } from '../api';
import { NoteStore, type Note, type NoteTransport } from './NoteStore';

const note: Note = { id: 'note-1', courseId: 'course-1', chapterId: null, title: '想法', content: '内容',
  videoPositionMs: null, version: 3, createdAt: '2026-09-14T00:00:00', updatedAt: '2026-09-14T00:00:00' };
function setup(handler: (path: string, body?: unknown, options?: RequestOptions) => Promise<unknown>) {
  const transport = vi.fn(handler);
  return { transport, store: new NoteStore('course-1', transport as NoteTransport, () => 'fixed-uuid') };
}

describe('Note 工作区', () => {
  it('名称搜索走后端并编码关键词，刷新复用已应用条件', async () => {
    const { store, transport } = setup(async () => []);
    store.setSearchInput('  Redis & 缓存  '); await store.search(); await store.load();
    expect(transport).toHaveBeenLastCalledWith('/api/learning/notes?courseId=course-1&limit=100&keyword=Redis+%26+%E7%BC%93%E5%AD%98');
    expect(store.getSnapshot().keyword).toBe('Redis & 缓存');
    await store.clearSearch();
    expect(transport).toHaveBeenLastCalledWith('/api/learning/notes?courseId=course-1&limit=100');
  });
  it('旧搜索响应不会覆盖新搜索结果', async () => {
    const pending: ((value: unknown) => void)[] = [];
    const { store } = setup(() => new Promise(resolve => pending.push(resolve)));
    store.setSearchInput('旧'); const old = store.search();
    store.setSearchInput('新'); const latest = store.search();
    pending[1]([{ ...note, title: '新' }]); await latest;
    pending[0]([{ ...note, title: '旧' }]); await old;
    expect(store.getSnapshot().notes[0].title).toBe('新');
  });
  it('明确400拒绝可修改内容；新尝试使用新幂等键', async () => {
    const transport = vi.fn(async (_path: string, _body?: unknown) => { throw new ApiError(400, '参数不合法'); });
    let keys = 0;
    const store = new NoteStore('course-1', transport as NoteTransport, () => `uuid-${++keys}`);
    store.create(); store.edit('title', '原名称'); await store.save();
    expect(store.getSnapshot().draft?.locked).toBe(false);
    store.edit('title', '新名称'); await store.save();
    expect(transport.mock.calls[1][1]).toMatchObject({ title: '新名称' });
    expect(transport.mock.calls[0][1]).toMatchObject({ idempotencyKey: 'web:note-create:uuid-1' });
    expect(transport.mock.calls[1][1]).toMatchObject({ idempotencyKey: 'web:note-create:uuid-2' });
    expect(store.getSnapshot().error).toContain('参数不合法');
  });
  it('已有未知创建结果时，后续400也不能解锁或更换幂等键', async () => {
    let attempts = 0;
    const { store, transport } = setup(async () => {
      if (++attempts === 1) throw new Error('超时');
      throw new ApiError(400, '请求被拒绝');
    });
    store.create(); store.edit('title', '保留'); await store.save(); await store.save();
    expect(store.getSnapshot().draft).toMatchObject({ locked: true, idempotencyKey: 'web:note-create:fixed-uuid' });
    expect(transport.mock.calls[0]).toEqual(transport.mock.calls[1]);
  });
  it('搜索状态下重命名后回查；刷新失败不伪装保存失败', async () => {
    let lists = 0;
    const { store } = setup(async (path, _body, options) => {
      if (path.includes('?')) {
        if (++lists === 1) return [note];
        throw new ApiError(503, '列表不可用');
      }
      return options ? { ...note, title: '新名', version: 4 } : note;
    });
    store.setSearchInput('想法'); await store.search();
    await store.open(note.id, 'rename'); store.edit('title', '新名'); await store.save();
    expect(store.getSnapshot()).toMatchObject({ status: 'error', draft: undefined, notice: 'Note 保存成功' });
    expect(store.getSnapshot().error).toContain('操作已成功，但搜索结果刷新失败');
  });
  it('503保持创建内容锁定，不允许更换参数', async () => {
    const { store } = setup(async () => { throw new ApiError(503, '服务不可用'); });
    store.create(); store.edit('title', '保留'); await store.save(); store.edit('title', '其他');
    expect(store.getSnapshot().draft).toMatchObject({ locked: true, title: '保留' });
  });
  it('只加载指定课程，失败不展示空列表结论', async () => {
    const { store, transport } = setup(async () => { throw new ApiError(503, '服务不可用'); });
    await store.load();
    expect(transport).toHaveBeenCalledWith('/api/learning/notes?courseId=course-1&limit=100');
    expect(store.getSnapshot()).toMatchObject({ status: 'error', error: '服务不可用' });
  });
  it('创建失败锁定请求，重试原UUID和参数，成功只显示一篇', async () => {
    let attempts = 0;
    const { store, transport } = setup(async () => {
      if (++attempts === 1) throw new Error('超时');
      return note;
    });
    store.create(); store.edit('title', '想法'); store.edit('content', '内容');
    await store.save();
    store.edit('title', '不允许改变');
    expect(store.getSnapshot().draft).toMatchObject({ locked: true, title: '想法' });
    await store.save();
    expect(transport.mock.calls[0]).toEqual(transport.mock.calls[1]);
    expect(transport.mock.calls[1][1]).toMatchObject({ idempotencyKey: 'web:note-create:fixed-uuid', videoPositionMs: null });
    expect(store.getSnapshot()).toMatchObject({ notes: [note], draft: undefined, status: 'success' });
  });
  it('打开读取最新版本，编辑PUT携带版本并更新列表', async () => {
    const { store, transport } = setup(async (_path, _body, options) => options?.method === 'PUT' ? { ...note, version: 4 } : note);
    await store.open(note.id); store.edit('content', '新内容'); await store.save();
    expect(transport).toHaveBeenLastCalledWith('/api/learning/notes/note-1',
      { title: '想法', content: '新内容', version: 3, videoPositionMs: null, tags: [] }, { method: 'PUT' });
    expect(store.getSnapshot().notes[0].version).toBe(4);
  });
  it('409保留草稿和原版本，不自动覆盖或重试', async () => {
    const { store, transport } = setup(async (_path, _body, options) => {
      if (options) throw new ApiError(409, '版本冲突');
      return note;
    });
    await store.open(note.id); store.edit('content', '保留我'); await store.save();
    expect(store.getSnapshot().draft).toMatchObject({ content: '保留我', conflict: true, original: { version: 3 } });
    await store.save();
    expect(transport).toHaveBeenCalledTimes(2);
  });
  it('重命名只PATCH名称和版本', async () => {
    const { store, transport } = setup(async () => note);
    await store.open(note.id, 'rename'); store.edit('title', '新名称'); await store.save();
    expect(transport).toHaveBeenLastCalledWith('/api/learning/notes/note-1/title',
      { title: '新名称', version: 3 }, { method: 'PATCH' });
  });
  it('删除携带版本，失败保留列表，成功再移除', async () => {
    let fail = true;
    const { store, transport } = setup(async (_path, _body, options) => {
      if (!options) return [note];
      if (fail) throw new ApiError(409, '版本冲突');
      return null;
    });
    await store.load(); await store.remove(note.id);
    expect(store.getSnapshot().notes).toEqual([note]);
    fail = false; await store.remove(note.id);
    expect(transport).toHaveBeenLastCalledWith('/api/learning/notes/note-1?version=3', undefined, { method: 'DELETE' });
    expect(store.getSnapshot().notes).toEqual([]);
  });
  it('卸载后忽略在途列表响应', async () => {
    let resolve!: (value: unknown) => void;
    const { store } = setup(() => new Promise(done => { resolve = done; }));
    const loading = store.load(); store.cancelPending(); resolve([note]); await loading;
    expect(store.getSnapshot().notes).toEqual([]);
  });
  it('仅上传合规图片并使用 FormData，不把文件塞进业务 DTO', async () => {
    const uploaded = { id: 'image-1', markdownUrl: '/api/learning/note-images/image-1', width: 800, height: 600 };
    const { store, transport } = setup(async () => uploaded);
    store.create();

    const file = new File(['png-content'], '架构图.png', { type: 'image/png' });
    await expect(store.uploadImage(file)).resolves.toEqual(uploaded);
    const body = transport.mock.calls[0][1];
    expect(body).toBeInstanceOf(FormData);
    expect((body as FormData).get('file')).toBe(file);
    expect(store.getSnapshot()).toMatchObject({ busy: false, error: '' });
  });
  it('上传前拦截不支持的类型和超过5MB的图片', async () => {
    const { store, transport } = setup(async () => undefined);
    store.create();

    await store.uploadImage(new File(['text'], 'readme.svg', { type: 'image/svg+xml' }));
    expect(store.getSnapshot().error).toContain('JPEG 或 PNG');
    await store.uploadImage(new File([new Uint8Array(5 * 1024 * 1024 + 1)], 'large.png', { type: 'image/png' }));
    expect(store.getSnapshot().error).toContain('不能超过5MB');
    expect(transport).not.toHaveBeenCalled();
  });
});
