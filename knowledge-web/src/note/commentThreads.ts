import type { NoteComment } from './NoteInteractionStore';

export interface NoteCommentThread {
  root: NoteComment;
  replies: NoteComment[];
}

export function buildCommentThreads(comments: NoteComment[]): NoteCommentThread[] {
  const roots = comments.filter(comment => comment.parentCommentId === null);
  const repliesByParent = new Map<string, NoteComment[]>();
  comments.filter(comment => comment.parentCommentId !== null).forEach(comment => {
    const replies = repliesByParent.get(comment.parentCommentId!) ?? [];
    replies.push(comment);
    repliesByParent.set(comment.parentCommentId!, replies);
  });
  const threads = roots.map(root => ({ root, replies: repliesByParent.get(root.id) ?? [] }));
  const visibleIds = new Set(roots.map(comment => comment.id));
  comments.filter(comment => comment.parentCommentId !== null && !visibleIds.has(comment.parentCommentId!))
    .forEach(reply => threads.push({ root: reply, replies: [] }));
  return threads;
}
