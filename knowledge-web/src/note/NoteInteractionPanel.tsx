import { Alert, Avatar, Button, Empty, Input, List, Pagination, Skeleton, Space, Tooltip, Typography } from 'antd';
import { CommentOutlined, DownOutlined, RobotOutlined, UpOutlined } from '@ant-design/icons';
import { useEffect, useLayoutEffect, useRef, useState } from 'react';
import { useLocation, useNavigate } from 'react-router';
import { useNoteInteraction } from './NoteInteractionContext';
import { formatNoteDate } from './formatNoteDate';
import { useUserDirectory } from '../profile/UserDirectoryContext';
import { useProfile } from '../profile/ProfileContext';
import { NoteReactionButton, NoteReactionStatButton } from './NoteReaction';
import { useAuth } from '../auth/AuthContext';
import { buildCommentThreads, type NoteCommentNode } from './commentThreads';

/** 统一评论输入框，正文仍以纯文本交给现有评论接口处理。 */
function CommentEditor({ id, value, onChange, disabled, placeholder, minRows = 2 }: {
  id?: string; value: string; onChange: (v: string) => void; disabled?: boolean; placeholder: string; minRows?: number;
}) {
  return <Input.TextArea id={id} value={value} disabled={disabled}
    placeholder={placeholder} autoSize={{ minRows, maxRows: 10 }}
    onChange={event => onChange(event.target.value)} />;
}

const COLLAPSED_COMMENT_HEIGHT = 154;

function CollapsibleCommentContent({ content }: { content: string }) {
  const contentRef = useRef<HTMLParagraphElement>(null);
  const [expanded, setExpanded] = useState(false);
  const [collapsible, setCollapsible] = useState(false);

  useLayoutEffect(() => {
    const element = contentRef.current;
    if (!element) return;
    const update = () => setCollapsible(element.scrollHeight > COLLAPSED_COMMENT_HEIGHT);
    update();
    const observer = new ResizeObserver(update);
    observer.observe(element);
    return () => observer.disconnect();
  }, [content]);

  return <div className="comment-content-wrap">
    <div className={expanded ? 'comment-content-clamp is-expanded' : 'comment-content-clamp'}>
      <p ref={contentRef} className="comment-content">{content}</p>
      {!expanded && collapsible ? <span className="comment-content-fade" aria-hidden="true" /> : null}
    </div>
    {collapsible ? <Button type="link" className="comment-content-toggle" icon={expanded ? <UpOutlined /> : <DownOutlined />}
      aria-expanded={expanded} onClick={() => setExpanded(value => !value)}>
      {expanded ? '收起' : '查看更多'}
    </Button> : null}
  </div>;
}

function InlineReplyBox({ avatarUrl, value, disabled, locked, busy, parentName, onChange, onCancel, onSubmit, onAskXiaozhi }: {
  avatarUrl?: string | null; value: string; disabled: boolean; locked: boolean; busy: boolean; parentName: string;
  onChange: (value: string) => void; onCancel: () => void; onSubmit: () => void; onAskXiaozhi: () => void;
}) {
  return <div className="inline-reply-box">
    <Avatar className="inline-reply-avatar" src={avatarUrl ?? undefined}>我</Avatar>
    <div className="inline-reply-body">
      <CommentEditor id="note-comment-reply-input" value={value} disabled={disabled}
        placeholder={`回复 @${parentName}`} minRows={1} onChange={onChange} />
      <div className="inline-reply-actions">
        {locked ? <span className="muted">上次结果未确认，重试会复用原内容</span> : null}
        <Button className="ask-xiaozhi-btn" icon={<span className="ask-xiaozhi-sparkle">✦</span>} onClick={onAskXiaozhi}>问小智</Button>
        <Button onClick={onCancel} disabled={busy}>取消</Button>
        <Button type="primary" loading={busy} disabled={!locked && !value.trim()} onClick={onSubmit}>回复</Button>
      </div>
    </div>
  </div>;
}

export function NoteInteractionPanel() {
  const { status, error, busyAction, engagement, comments, input, replyInput, parentCommentId,
    commentLocked, page, total, store } = useNoteInteraction();
  const { profiles, store: directory } = useUserDirectory();
  const { profile } = useProfile();
  const { status: authStatus } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const authenticated = authStatus === 'authenticated';
  const commentStartRef = useRef<HTMLDivElement>(null);
  const [floatingActions, setFloatingActions] = useState(true);
  const [expandedReplyIds, setExpandedReplyIds] = useState<Set<string>>(() => new Set());
  const requireLogin = () => navigate('/login', { state: { from: location.pathname } });
  const threads = buildCommentThreads(comments);
  const displayName = (userId?: string | null) => userId ? profiles[userId]?.nickname ?? userId : '未知用户';
  const commentAuthorName = (comment?: typeof comments[number]) =>
    comment?.authorType === 'AGENT' ? '小智' : displayName(comment?.authorId);
  const insertXiaozhiMention = () => {
    if (!authenticated) { requireLogin(); return; }
    store.setRootInput(`${input}@小智`);
    document.getElementById('note-comment-input')?.focus();
  };
  const submitComment = () => authenticated ? void store.submitComment() : requireLogin();
  const submitRootComment = () => authenticated ? void store.submitRootComment() : requireLogin();
  const toggleReplies = (commentId: string) => setExpandedReplyIds(previous => {
    const next = new Set(previous);
    if (next.has(commentId)) next.delete(commentId); else next.add(commentId);
    return next;
  });
  const replyTo = (commentId: string) => {
    if (!authenticated) { requireLogin(); return; }
    setExpandedReplyIds(previous => new Set(previous).add(commentId));
    store.replyTo(commentId);
  };
  const renderComment = (comment: typeof comments[number], nested = false, replyCount = 0, repliesExpanded = false) => {
    const parent = comment.parentCommentId ? comments.find(item => item.id === comment.parentCommentId) : undefined;
    const isAgent = comment.authorType === 'AGENT';
    const isAuthor = !isAgent && comment.authorId && comment.authorId === store.noteAuthorId;
    const authorName = isAgent ? '小智' : displayName(comment.authorId);
    const avatar = isAgent
      ? <Avatar className="comment-agent-avatar" icon={<RobotOutlined />} />
      : <Avatar src={profiles[comment.authorId]?.avatarUrl ?? undefined}>{authorName.slice(0, 1).toUpperCase()}</Avatar>;
    return <div className={nested ? 'note-comment note-comment-reply' : 'note-comment'}>
      <List.Item.Meta
        avatar={avatar}
        title={<div className="comment-heading"><span className="comment-author">{authorName}</span>
          {isAgent ? <span className="comment-agent-tag" title="小智由 AI 生成回复" aria-label="AI 生成">AI</span> : null}
          {isAuthor ? <span className="comment-author-tag" title="笔记作者">作者</span> : null}
          {nested ? <span className="comment-reply-target">回复 <span>@{commentAuthorName(parent)}</span></span> : null}</div>}
        description={<>
          <time className="comment-time" dateTime={comment.createdAt}>{formatNoteDate(comment.createdAt)}</time>
          <CollapsibleCommentContent content={comment.content} />
          <div className="note-comment-actions">
            <NoteReactionStatButton kind="like" count={comment.likeCount} active={comment.liked}
              disabled={busyAction === 'comment-like'} busy={busyAction === 'comment-like'}
              onClick={() => authenticated ? void store.toggleCommentLike(comment.id) : requireLogin()} />
            {replyCount ? <Button type="link" className="comment-replies-toggle" onClick={() => toggleReplies(comment.id)}
              icon={<CommentOutlined />}>
              {repliesExpanded ? '隐藏回复' : `展开 ${replyCount} 条回复`}
            </Button> : null}
            <Button type="link" className="comment-reply-action" onClick={() => replyTo(comment.id)}>回复</Button>
            {comment.owned ? <Button type="link" danger loading={busyAction === 'delete'}
              onClick={() => void store.deleteComment(comment.id)}>删除</Button> : null}
          </div>
        </>} />
    </div>;
  };
  const countDescendants = (node: NoteCommentNode): number =>
    node.replies.reduce((sum, r) => sum + 1 + countDescendants(r), 0);
  const expandLimit = new Map<string, number>();
  const renderCommentNode = (node: NoteCommentNode, nested = false) => {
    const repliesExpanded = expandedReplyIds.has(node.comment.id);
    const totalReplies = countDescendants(node);
    const limit = expandLimit.get(node.comment.id) ?? 10;
    const visibleReplies = repliesExpanded ? node.replies.slice(0, limit) : [];
    return <>
      <div className={nested ? 'note-comment-node is-reply' : 'note-comment-node'}>
        {renderComment(node.comment, nested, nested ? 0 : totalReplies, nested ? false : repliesExpanded)}
      </div>
      {parentCommentId === node.comment.id ? <InlineReplyBox avatarUrl={profile?.avatarUrl} value={replyInput}
        disabled={commentLocked || busyAction === 'comment'} locked={commentLocked} busy={busyAction === 'comment'}
        parentName={commentAuthorName(node.comment)} onChange={store.setInput}
        onCancel={() => store.replyTo(null)} onSubmit={submitComment}
        onAskXiaozhi={() => store.setInput(`${replyInput}@小智`)} /> : null}
      {!nested && repliesExpanded ? <div className="note-comment-children">
        {visibleReplies.map(reply => <div key={reply.comment.id}>{renderCommentNode(reply, true)}</div>)}
        {node.replies.length > limit ? <Button type="link" size="small" onClick={() => expandLimit.set(node.comment.id, limit + 10)}>加载更多回复</Button> : null}
      </div> : null}
      {nested && node.replies.length > 0 ? <div className="note-comment-children is-nested">
        {node.replies.map(reply => <div key={reply.comment.id}>{renderCommentNode(reply, true)}</div>)}
      </div> : null}
    </>;
  };
  useEffect(() => { void directory.ensure(comments.map(comment => comment.authorId)); }, [comments, directory]);
  if (status === 'idle' || status === 'loading') return <Skeleton active paragraph={{ rows: 3 }} />;
  return <section className="note-interaction" aria-label="笔记互动">
    {error ? <Alert type="error" showIcon message={error}
      action={status === 'error' ? <Button onClick={() => void store.load()}>重试</Button> : undefined} /> : null}
    {engagement ? <Space wrap className="note-actions is-floating">
      <NoteReactionButton kind="like" active={engagement.liked} count={engagement.likeCount}
        disabled={authenticated && busyAction !== null} busy={busyAction === 'like'}
        onClick={() => authenticated ? void store.toggleLike() : requireLogin()} />
      <NoteReactionButton kind="favorite" active={engagement.favorited} count={engagement.favoriteCount}
        disabled={authenticated && busyAction !== null} busy={busyAction === 'favorite'}
        onClick={() => authenticated ? void store.toggleFavorite() : requireLogin()} />
      <NoteReactionButton kind="comment" count={engagement.commentCount}
        onClick={() => authenticated ? commentStartRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' }) : requireLogin()} />
    </Space> : null}
    <div ref={commentStartRef} className="comment-section-start">
      <Typography.Title level={4} className="comment-section-title">
        <CommentOutlined aria-hidden="true" />
        <span>评论（{engagement?.commentCount ?? total}）</span>
      </Typography.Title>
      <div className="comment-composer">
        <CommentEditor id="note-comment-input" value={input} disabled={!authenticated || commentLocked || busyAction === 'comment'}
          minRows={3} placeholder={authenticated ? '请输入评论…' : '登录后参与评论'} onChange={store.setRootInput} />
        <div className="comment-composer-footer">
          <div className="comment-composer-tools">
            <span className="comment-character-count" aria-live="polite">{input.length} / 1000</span>
          </div>
          <Space size={10}>
            {commentLocked ? <span className="muted">上次结果未确认，重试会复用同一幂等键与内容</span> : null}
            <Tooltip title="在评论中提及小智，由它异步答疑">
              <Button type="primary" disabled={authenticated && (commentLocked || busyAction === 'comment')}
                className="ask-xiaozhi-btn"
                icon={<span className="ask-xiaozhi-sparkle">✦</span>}
                onClick={insertXiaozhiMention}>问小智</Button>
            </Tooltip>
            <Button type="primary" loading={busyAction === 'comment'}
              disabled={authenticated && !commentLocked && !input.trim()}
              onClick={submitRootComment}>
              {!authenticated ? '登录后评论' : commentLocked ? '重试' : '发送'}
            </Button>
          </Space>
        </div>
      </div>
    </div>
    {comments.length ? <List className="note-comment-threads" dataSource={threads} renderItem={thread => <List.Item className="note-comment-thread">
      {renderCommentNode(thread.root)}
    </List.Item>} /> : <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="还没有评论，来发表第一条吧" />}
    {total > 50 ? <Pagination current={page} total={total} pageSize={50} showSizeChanger={false}
      onChange={next => void store.loadComments(next)} /> : null}
  </section>;
}
