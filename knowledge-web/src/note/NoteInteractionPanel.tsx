import { Alert, Button, Empty, Input, List, Pagination, Skeleton, Space, Typography } from 'antd';
import { useEffect } from 'react';
import { useLocation, useNavigate } from 'react-router';
import { useNoteInteraction } from './NoteInteractionContext';
import { formatNoteDate } from './formatNoteDate';
import { UserIdentity } from '../profile/UserIdentity';
import { useUserDirectory } from '../profile/UserDirectoryContext';
import { NoteReactionButton } from './NoteReaction';
import { useAuth } from '../auth/AuthContext';
import { buildCommentThreads } from './commentThreads';

export function NoteInteractionPanel() {
  const { status, error, busyAction, engagement, comments, input, parentCommentId,
    commentLocked, page, total, store } = useNoteInteraction();
  const { profiles, store: directory } = useUserDirectory();
  const { status: authStatus } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const authenticated = authStatus === 'authenticated';
  const requireLogin = () => navigate('/login', { state: { from: location.pathname } });
  const threads = buildCommentThreads(comments);
  const displayName = (userId?: string | null) => userId ? profiles[userId]?.nickname ?? userId : '未知用户';
  const renderComment = (comment: typeof comments[number], nested = false) => {
    const parent = comment.parentCommentId ? comments.find(item => item.id === comment.parentCommentId) : undefined;
    return <List.Item className={nested ? 'note-comment note-comment-reply' : 'note-comment'}
      actions={[
        ...(!authenticated || nested ? [] : [<Button key="reply" type="link" onClick={() => store.replyTo(comment.id)}>回复</Button>]),
        ...(comment.owned ? [<Button key="delete" type="link" danger loading={busyAction === 'delete'}
          onClick={() => void store.deleteComment(comment.id)}>删除</Button>] : []),
      ]}>
      <List.Item.Meta
        title={<Space size={8}><UserIdentity userId={comment.authorId} />{nested ? <span className="comment-reply-target">
          回复 <span>@{displayName(parent?.authorId)}</span></span> : null}</Space>}
        description={<><p className="comment-content">{comment.content}</p>
          <time dateTime={comment.createdAt}>{formatNoteDate(comment.createdAt)}</time></>} />
    </List.Item>;
  };
  useEffect(() => { void directory.ensure(comments.map(comment => comment.authorId)); }, [comments, directory]);
  if (status === 'idle' || status === 'loading') return <Skeleton active paragraph={{ rows: 3 }} />;
  return <section className="note-interaction" aria-label="笔记互动">
    {error ? <Alert type="error" showIcon message={error}
      action={status === 'error' ? <Button onClick={() => void store.load()}>重试</Button> : undefined} /> : null}
    {engagement ? <Space wrap className="note-actions">
      <NoteReactionButton kind="like" active={engagement.liked} count={engagement.likeCount}
        disabled={authenticated && busyAction !== null} busy={busyAction === 'like'}
        onClick={() => authenticated ? void store.toggleLike() : requireLogin()} />
      <NoteReactionButton kind="favorite" active={engagement.favorited} count={engagement.favoriteCount}
        disabled={authenticated && busyAction !== null} busy={busyAction === 'favorite'}
        onClick={() => authenticated ? void store.toggleFavorite() : requireLogin()} />
      <NoteReactionButton kind="comment" count={engagement.commentCount}
        onClick={() => authenticated ? document.getElementById('note-comment-input')?.focus() : requireLogin()} />
    </Space> : null}
    <div className="comment-composer">
      {parentCommentId ? <div className="comment-reply-hint">正在回复 @{displayName(comments.find(comment => comment.id === parentCommentId)?.authorId)}
        <Button type="link" onClick={() => store.replyTo(null)}>取消回复</Button></div> : null}
      <Input.TextArea id="note-comment-input" aria-label="评论内容" value={input} maxLength={1000} showCount autoSize={{ minRows: 2, maxRows: 6 }}
        disabled={!authenticated || commentLocked || busyAction === 'comment'}
        placeholder={authenticated ? '分享你的想法，友善交流' : '登录后参与评论'}
        onChange={event => store.setInput(event.target.value)} />
      <Space>
        <Button type="primary" loading={busyAction === 'comment'}
          onClick={() => authenticated ? void store.submitComment() : requireLogin()}>
          {!authenticated ? '登录后评论' : commentLocked ? '重试原评论' : '发表评论'}
        </Button>
        {commentLocked ? <span className="muted">上次结果未确认，重试会复用同一幂等键与内容</span> : null}
      </Space>
    </div>
    <Typography.Title level={4}>全部评论</Typography.Title>
    {comments.length ? <List className="note-comment-threads" dataSource={threads} renderItem={thread => <li className="note-comment-thread">
      {renderComment(thread.root)}
      {thread.replies.length ? <List className="note-comment-replies" dataSource={thread.replies}
        renderItem={reply => renderComment(reply, true)} /> : null}
    </li>} /> : <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="还没有评论，来发表第一条吧" />}
    {total > 50 ? <Pagination current={page} total={total} pageSize={50} showSizeChanger={false}
      onChange={next => void store.loadComments(next)} /> : null}
  </section>;
}
