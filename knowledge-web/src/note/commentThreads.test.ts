import { expect, it } from 'vitest';
import type { NoteComment } from './NoteInteractionStore';
import { buildCommentThreads } from './commentThreads';

const comment = (id: string, parentCommentId: string | null): NoteComment => ({
  id, noteId: 'note-1', authorId: `user-${id}`, parentCommentId, content: id,
  owned: false, version: 0, createdAt: '', updatedAt: '',
});

it('将回复收进对应一级评论并保持接口顺序', () => {
  const result = buildCommentThreads([
    comment('root-1', null), comment('reply-1', 'root-1'),
    comment('root-2', null), comment('reply-2', 'root-1'),
  ]);
  expect(result.map(thread => ({ root: thread.root.id, replies: thread.replies.map(reply => reply.id) }))).toEqual([
    { root: 'root-1', replies: ['reply-1', 'reply-2'] },
    { root: 'root-2', replies: [] },
  ]);
});

it('分页中父评论缺席时仍展示回复内容', () => {
  expect(buildCommentThreads([comment('reply-1', 'root-on-previous-page')])[0].root.id).toBe('reply-1');
});
