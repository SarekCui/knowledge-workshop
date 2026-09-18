import { expect, it } from 'vitest';
import type { NoteComment } from './NoteInteractionStore';
import { buildCommentThreads } from './commentThreads';

const comment = (id: string, parentCommentId: string | null): NoteComment => ({
  id, noteId: 'note-1', authorId: `user-${id}`, authorType: 'USER', parentCommentId, content: id,
  likeCount: 0, liked: false, owned: false, version: 0, createdAt: '', updatedAt: '',
});

it('将任意层级回复收进对应父评论并保持接口顺序', () => {
  const result = buildCommentThreads([
    comment('root-1', null), comment('reply-1', 'root-1'),
    comment('root-2', null), comment('reply-2', 'root-1'), comment('reply-3', 'reply-1'),
  ]);
  expect(result[0].root.comment.id).toBe('root-1');
  expect(result[0].root.replies.map(reply => reply.comment.id)).toEqual(['reply-1', 'reply-2']);
  expect(result[0].root.replies[0].replies.map(reply => reply.comment.id)).toEqual(['reply-3']);
  expect(result[1].root.comment.id).toBe('root-2');
});

it('分页中父评论缺席时仍展示回复内容', () => {
  expect(buildCommentThreads([comment('reply-1', 'root-on-previous-page')])[0].root.comment.id).toBe('reply-1');
});
