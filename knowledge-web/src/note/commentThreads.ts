import type { NoteComment } from './NoteInteractionStore';

export interface NoteCommentThread {
  root: NoteCommentNode;
}

export interface NoteCommentNode {
  comment: NoteComment;
  replies: NoteCommentNode[];
}

export function buildCommentThreads(comments: NoteComment[]): NoteCommentThread[] {
  const nodesById = new Map(comments.map(comment => [comment.id, { comment, replies: [] as NoteCommentNode[] }]));
  const threads: NoteCommentThread[] = [];

  comments.forEach(comment => {
    const node = nodesById.get(comment.id)!;
    const parent = comment.parentCommentId ? nodesById.get(comment.parentCommentId) : undefined;
    if (parent) {
      parent.replies.push(node);
    } else {
      // 当前页未包含父评论时，仍保证这条回复可见；跨页不会伪造不完整的祖先链。
      threads.push({ root: node });
    }
  });
  return threads;
}
