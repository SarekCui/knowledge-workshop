import { expect, it, vi } from 'vitest';
import { NoteBrowseStore } from './NoteBrowseStore';
import { NoteStore } from './NoteStore';
import { returnPath } from '../auth/returnPath';

const note = { id: 'note-1', courseId: null, chapterId: null, title: '知识分享', content: '正文', version: 0,
  videoPositionMs: null, createdAt: '', updatedAt: '', authorId: 'author', status: 'DRAFT' as const };
it('追加失败保留列表，重试同页且合并时去重', async () => {
  const transport = vi.fn().mockResolvedValueOnce({ items: [note], total: 3, pageNo: 1 })
    .mockRejectedValueOnce(new Error('断网'))
    .mockResolvedValueOnce({ items: [note, { ...note, id: 'note-2' }], total: 3, pageNo: 2 });
  const store = new NoteBrowseStore(transport);
  await store.load(); await store.loadMore();
  expect(store.getSnapshot()).toMatchObject({ notes: [note], page: 1, status: 'success', moreError: '断网' });
  await store.loadMore();
  expect(transport.mock.calls[1][0]).toEqual(transport.mock.calls[2][0]);
  expect(store.getSnapshot().notes.map(item => item.id)).toEqual(['note-1', 'note-2']);
});
it('公开搜索仅调用public契约，编码条件且切页保留条件', async () => {
  const transport = vi.fn().mockResolvedValue({ items: [], total: 40, pageNo: 1 });
  const store = new NoteBrowseStore(transport, undefined, 'course-1');
  store.setInput(' Redis & 缓存 '); await store.search(); await store.load(2);
  expect(transport.mock.calls[1][0]).toBe('/api/learning/notes/public?pageNo=2&pageSize=20&keyword=Redis+%26+%E7%BC%93%E5%AD%98&courseId=course-1&sort=LATEST');
});
it('点击技术标签后使用精确标签条件并支持清除', async () => {
  const transport = vi.fn().mockResolvedValue({ items: [], total: 0, pageNo: 1 });
  const store = new NoteBrowseStore(transport);
  await store.selectTag('Spring Boot');
  expect(transport).toHaveBeenLastCalledWith('/api/learning/notes/public?pageNo=1&pageSize=20&tag=Spring+Boot&sort=LATEST');
  await store.clearTag();
  expect(transport).toHaveBeenLastCalledWith('/api/learning/notes/public?pageNo=1&pageSize=20&sort=LATEST');
});
it('公开详情失败不回退私人接口', async () => {
  const transport = vi.fn().mockRejectedValue(new Error('不存在'));
  const store = new NoteBrowseStore(transport, 'note-1'); await store.load();
  expect(transport).toHaveBeenCalledExactlyOnceWith('/api/learning/notes/public/note-1');
  expect(store.getSnapshot()).toMatchObject({ status: 'error', note: undefined });
});
it('忽略旧公开列表响应及退出后的结果', async () => {
  let resolve!: (value: unknown) => void;
  const transport = vi.fn().mockImplementationOnce(() => new Promise(done => { resolve = done; }))
    .mockResolvedValueOnce({ items: [], total: 0, pageNo: 1 });
  const store = new NoteBrowseStore(transport); const old = store.load(); await store.search();
  resolve({ items: [note], total: 1, pageNo: 1 }); await old;
  expect(store.getSnapshot().notes).toEqual([]);
});
it('独立创作默认无课程，首次未知结果锁定UUID与内容', async () => {
  const transport = vi.fn().mockRejectedValue(new Error('超时'));
  const store = new NoteStore(null, transport, () => 'uuid-1', true);
  store.create(); store.edit('title', '知识分享'); store.edit('content', '正文'); await store.save();
  store.selectCourse('course-1'); await store.save();
  expect(transport.mock.calls[0][1]).toEqual(transport.mock.calls[1][1]);
  expect(transport.mock.calls[0][1]).toMatchObject({ courseId: null, idempotencyKey: 'web:note-create:uuid-1' });
});
it('课程选择进入创建参数，发布带版本并刷新本人分页', async () => {
  const transport = vi.fn().mockResolvedValueOnce({ ...note, courseId: 'course-1' })
    .mockResolvedValueOnce({ items: [{ ...note, courseId: 'course-1' }], total: 1, pageNo: 1 })
    .mockResolvedValueOnce({ ...note, status: 'PUBLIC', version: 1 })
    .mockResolvedValueOnce({ items: [{ ...note, status: 'PUBLIC', version: 1 }], total: 1, pageNo: 1 });
  const store = new NoteStore(null, transport, () => 'uuid-1', true);
  store.create(); store.selectCourse('course-1'); store.edit('title', '知识分享'); await store.save();
  expect(transport.mock.calls[0][1]).toMatchObject({ courseId: 'course-1' });
  await store.changeStatus('note-1', 'PUBLIC');
  expect(transport.mock.calls[2]).toEqual(['/api/learning/notes/note-1/status', { status: 'PUBLIC', version: 0 }, { method: 'PATCH' }]);
  expect(store.getSnapshot().notes[0]?.status).toBe('PUBLIC');
});
it('登录回跳支持笔记路由但不支持任意深路径', () => {
  for (const path of ['/notes', '/notes/mine', '/notes/liked', '/notes/favorites', '/notes/note-1', '/notes/course/course-1']) expect(returnPath(path)).toBe(path);
  expect(returnPath('/notes/course/course-1/other')).toBe('/learning');
});
