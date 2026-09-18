import { Alert, Button, Empty, Segmented, Skeleton, Tag, Typography } from 'antd';
import { useEffect } from 'react';
import { Link } from 'react-router';
import { useNoteBrowse } from './NoteBrowseContext';
import { NoteBrowseSearch } from './NoteBrowseSearch';
import { NoteFeed } from './NoteFeed';
import { formatNoteDate } from './formatNoteDate';
import { NoteInteractionProvider } from './NoteInteractionContext';
import { NoteInteractionPanel } from './NoteInteractionPanel';
import { UserIdentity } from '../profile/UserIdentity';
import { useUserDirectory } from '../profile/UserDirectoryContext';
import { CalendarOutlined, TagOutlined } from '@ant-design/icons';
import { MarkdownDocument } from './MarkdownDocument';

function NoteAuthorMeta({ note }: { note: { authorId?: string; publishedAt?: string | null; courseId?: string | null } }) {
  const { profiles } = useUserDirectory();
  const name = note.authorId ? (profiles[note.authorId]?.nickname ?? note.authorId) : '未知用户';
  const avatarUrl = note.authorId ? profiles[note.authorId]?.avatarUrl : undefined;
  const initial = name.slice(0, 1).toUpperCase();
  return <div className="note-author-meta">
    <span className="note-author-avatar">
      {avatarUrl ? <img src={avatarUrl} alt="" /> : initial}
    </span>
    <div className="note-author-info">
      <span className="note-author-name">{name}</span>
      <div className="note-meta-sub">
        <CalendarOutlined />{formatNoteDate(note.publishedAt)}
        <span className="note-meta-item"><TagOutlined />{note.courseId ? '课程笔记' : '知识分享'}</span>
      </div>
    </div>
  </div>;
}

export function NoteBrowsePage({ embedded = false }: { embedded?: boolean }) {
  const { status, error, notes, note, total, keyword, tag, sort, store, loadingMore, moreError } = useNoteBrowse();
  const { store: directory } = useUserDirectory();
  useEffect(() => { void directory.ensure([note?.authorId, ...notes.map(item => item.authorId)]); }, [directory, note, notes]);
  const collectionTitle = store.collection === 'liked' ? '我点赞的笔记'
    : store.collection === 'favorites' ? '我收藏的笔记' : null;
  const channelTags = [...new Set(notes.flatMap(item => item.tags ?? []))].slice(0, 10);
  return <section className={store.noteId ? 'note-detail-page' : undefined}>
    {!store.noteId && !embedded ? <header className="page-heading">
      <h1 className={store.collection === 'public' && !store.courseId ? 'visually-hidden' : undefined}>
        {collectionTitle ?? (store.courseId ? '课程下的技术 Note' : '技术 Note')}
      </h1>
      {store.collection === 'public' ? <>
        <div className="note-discovery-toolbar"><NoteBrowseSearch />
          <Segmented aria-label="笔记排序" value={sort} options={[{ label: '最新', value: 'LATEST' }, { label: '热门', value: 'HOT' }]}
            onChange={value => void store.setSort(value as 'LATEST' | 'HOT')} /></div>
        {!store.courseId ? <nav className="note-channel-bar" aria-label="技术频道">
          <button type="button" className={!tag ? 'is-active' : undefined} onClick={() => void store.clearTag()}>推荐</button>
          {channelTags.map(item => <button type="button" key={item} className={tag === item ? 'is-active' : undefined}
            onClick={() => void store.selectTag(item)}>{item}</button>)}
        </nav> : null}
        {tag && !channelTags.includes(tag) ? <Tag className="note-active-tag" color="blue" closable
          onClose={() => void store.clearTag()}>技术标签：{tag}</Tag> : null}</> : null}
    </header> : null}
    {status === 'error' ? <Alert type="error" showIcon message={error} action={<Button onClick={() => void store.load()}>重试</Button>} />
      : status === 'loading' || status === 'idle' ? <div className="note-feed-skeleton" aria-label="正在加载笔记" role="status">
        {Array.from({ length: 6 }, (_, index) => <div key={index}><Skeleton active paragraph={{ rows: 3 + index % 3 }} /></div>)}
      </div>
      : store.noteId && note ? <article className="public-note-detail">
        <Link className="note-detail-back" aria-label="返回笔记列表"
          to={note.courseId ? `/notes/course/${encodeURIComponent(note.courseId)}` : '/notes'}>
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M15 5 8 12l7 7" /></svg>
        </Link>
        <Typography.Title level={2}>{note.title}</Typography.Title>
        <NoteAuthorMeta note={note} />
        {note.tags?.length ? <div className="note-tag-list" aria-label="技术标签">
          {note.tags.map(item => <Tag key={item}>#{item}</Tag>)}
        </div> : null}
        {note.courseId ? <Link to={`/notes/course/${encodeURIComponent(note.courseId)}`}>浏览关联课程的全部公开笔记 →</Link> : null}
        <MarkdownDocument content={note.content} showToc />
        <NoteInteractionProvider noteAuthorId={note.authorId}><NoteInteractionPanel /></NoteInteractionProvider>
      </article> : <>
        {notes.length ? <NoteFeed />
          : <Empty description={keyword || tag ? '没有找到匹配的公开笔记' : '暂无公开笔记，可前往我的笔记创作并发布'} />}
        <div className="note-load-more">
          {moreError ? <Alert type="error" showIcon message={moreError} /> : null}
          {notes.length < total ? <Button loading={loadingMore} onClick={() => void store.loadMore()}>
            {moreError ? '重试加载' : '加载更多'}</Button> : notes.length ? <span className="muted">已经看到全部笔记</span> : null}
        </div>
      </>}
  </section>;
}
