import { expect, it, vi } from 'vitest';
import { ApiError } from '../api';
import { NoteInteractionStore, type NoteComment, type NoteEngagement } from './NoteInteractionStore';
import type { NoteTransport } from './NoteStore';
import { NoteBrowseStore } from './NoteBrowseStore';

const engagement: NoteEngagement = { likeCount: 2, favoriteCount: 1, commentCount: 0, liked: false, favorited: false };
const comment: NoteComment = { id: 'comment-1', noteId: 'note-1', authorId: 'user-1', authorType: 'USER', parentCommentId: null,
  content: '很有帮助', likeCount: 0, liked: false, owned: true, version: 0, createdAt: '', updatedAt: '' };

it('评论点赞乐观更新，失败时恢复原状态', async () => {
  const transport = vi.fn()
    .mockResolvedValueOnce(engagement)
    .mockResolvedValueOnce({ items: [comment], pageNo: 1, pageSize: 50, total: 1 })
    .mockRejectedValueOnce(new Error('网络失败'));
  const store = new NoteInteractionStore('note-1', transport);
  await store.load();
  transport.mockClear();

  await store.toggleCommentLike(comment.id);

  expect(transport).toHaveBeenCalledWith('/api/learning/notes/comments/comment-1/likes', undefined, { method: 'PUT' });
  expect(store.getSnapshot().comments[0]).toMatchObject({ liked: false, likeCount: 0 });
});

it('首屏并行加载互动状态和评论', async () => {
  const transport = vi.fn(path => Promise.resolve(path.endsWith('/engagement') ? engagement
    : { items: [], pageNo: 1, pageSize: 50, total: 0 }));
  const store = new NoteInteractionStore('note-1', transport as NoteTransport);
  const loading = store.load();
  expect(transport).toHaveBeenCalledTimes(2);
  await loading;
  expect(store.getSnapshot()).toMatchObject({ status: 'success', engagement, comments: [] });
});

it('点赞和取消点赞使用幂等PUT与DELETE', async () => {
  const transport = vi.fn()
    .mockResolvedValueOnce(engagement)
    .mockResolvedValueOnce({ items: [], total: 0, pageNo: 1 })
    .mockResolvedValueOnce({ ...engagement, liked: true, likeCount: 3 })
    .mockResolvedValueOnce(engagement);
  const store = new NoteInteractionStore('note/1', transport);
  await store.load();
  transport.mockClear();
  await store.toggleLike();
  await store.toggleLike();
  expect(transport.mock.calls).toEqual([
    ['/api/learning/notes/public/note%2F1/likes', undefined, { method: 'PUT' }],
    ['/api/learning/notes/public/note%2F1/likes', undefined, { method: 'DELETE' }],
  ]);
});

it('详情点赞后立即同步已经打开的列表状态', async () => {
  const listedNote = { id: 'note-sync', courseId: null, chapterId: null, title: '同步', content: '正文',
    version: 0, videoPositionMs: null, createdAt: '', updatedAt: '', likeCount: 2, favoriteCount: 1,
    commentCount: 0, liked: false, favorited: false };
  const transport = vi.fn((path: string) => {
    if (path.includes('?')) return Promise.resolve({ items: [listedNote], total: 1, pageNo: 1 });
    if (path.endsWith('/engagement')) return Promise.resolve(engagement);
    if (path.endsWith('/comments')) return Promise.resolve({ items: [], total: 0, pageNo: 1 });
    return Promise.resolve({ ...engagement, liked: true, likeCount: 3 });
  }) as NoteTransport;
  const detailTransport = ((path, body, options) => transport(path, body, options)) as NoteTransport;
  const browse = new NoteBrowseStore(transport);
  const detail = new NoteInteractionStore('note-sync', detailTransport, undefined, transport);
  await browse.load();
  await detail.load();
  await detail.toggleLike();
  expect(browse.getSnapshot().notes[0]).toMatchObject({ liked: true, likeCount: 3 });
  browse.cancel();
});

it('收藏立即反馈，阻止连续点击，失败恢复原状态', async () => {
  let reject!: (error: Error) => void;
  const transport = vi.fn().mockResolvedValueOnce(engagement)
    .mockResolvedValueOnce({ items: [], total: 0, pageNo: 1 })
    .mockImplementationOnce(() => new Promise((_, fail) => { reject = fail; }));
  const store = new NoteInteractionStore('note-1', transport);
  await store.load();
  const pending = store.toggleFavorite();
  expect(store.getSnapshot().engagement).toMatchObject({ favorited: true, favoriteCount: 2 });
  await store.toggleFavorite();
  expect(transport).toHaveBeenCalledTimes(3);
  reject(new Error('网络失败')); await pending;
  expect(store.getSnapshot()).toMatchObject({ engagement, error: '网络失败', busyAction: null });
});

it('评论结果未知时锁定内容并复用UUID重试', async () => {
  const transport = vi.fn().mockRejectedValueOnce(new Error('超时')).mockResolvedValueOnce(comment);
  const store = new NoteInteractionStore('note-1', transport, () => 'request-1');
  store.setInput('很有帮助');
  await store.submitComment();
  store.setInput('不应覆盖');
  await store.submitComment();
  expect(transport.mock.calls[0][1]).toEqual(transport.mock.calls[1][1]);
  expect(store.getSnapshot()).toMatchObject({ commentLocked: false, input: '', total: 1 });
});

it('明确的参数错误允许修正并换新UUID', async () => {
  let key = 0;
  const transport = vi.fn().mockRejectedValueOnce(new ApiError(400, '参数错误')).mockResolvedValueOnce(comment);
  const store = new NoteInteractionStore('note-1', transport, () => `request-${++key}`);
  store.setInput('旧内容'); await store.submitComment();
  store.setInput('新内容'); await store.submitComment();
  expect(transport.mock.calls[0][1]).toMatchObject({ clientRequestId: 'request-1', content: '旧内容' });
  expect(transport.mock.calls[1][1]).toMatchObject({ clientRequestId: 'request-2', content: '新内容' });
});

it('回复评论时携带被回复评论 ID，并在成功后关闭内联回复框', async () => {
  const reply = { ...comment, id: 'reply-1', parentCommentId: 'comment-1', content: '谢谢你的反馈' };
  const transport = vi.fn().mockResolvedValue(reply);
  const store = new NoteInteractionStore('note-1', transport, () => 'reply-request-1');
  store.replyTo('comment-1');
  store.setInput('谢谢你的反馈');

  await store.submitComment();

  expect(transport).toHaveBeenCalledWith('/api/learning/notes/public/note-1/comments', {
    clientRequestId: 'reply-request-1', parentCommentId: 'comment-1', content: '谢谢你的反馈',
  });
  expect(store.getSnapshot()).toMatchObject({ parentCommentId: null, replyInput: '', commentLocked: false });
});

it('打开回复框后保留主评论草稿，并与回复草稿分开编辑', async () => {
  const transport = vi.fn().mockResolvedValue(comment);
  const store = new NoteInteractionStore('note-1', transport, () => 'root-request-1');
  store.setInput('主评论草稿');
  store.replyTo('comment-1');
  store.setInput('回复草稿');

  expect(store.getSnapshot()).toMatchObject({
    input: '主评论草稿', replyInput: '回复草稿', parentCommentId: 'comment-1',
  });

  await store.submitRootComment();

  expect(transport).toHaveBeenCalledWith('/api/learning/notes/public/note-1/comments', {
    clientRequestId: 'root-request-1', parentCommentId: null, content: '主评论草稿',
  });
  expect(store.getSnapshot()).toMatchObject({ input: '', replyInput: '回复草稿', parentCommentId: 'comment-1' });
});
