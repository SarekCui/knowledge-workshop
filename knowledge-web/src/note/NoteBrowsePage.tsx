import { Alert, Button, Empty, Segmented, Skeleton, Space, Tag, Typography } from 'antd';
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
import { MarkdownDocument } from './MarkdownDocument';

export function NoteBrowsePage() {
  const { status, error, notes, note, total, keyword, tag, sort, store, loadingMore, moreError } = useNoteBrowse();
  const { store: directory } = useUserDirectory();
  useEffect(() => { void directory.ensure([note?.authorId, ...notes.map(item => item.authorId)]); }, [directory, note, notes]);
  const collectionTitle = store.collection === 'liked' ? '我点赞的笔记'
    : store.collection === 'favorites' ? '我收藏的笔记' : null;
  return <section>
    <header className="page-heading">
      <Space wrap><Link to="/notes">发现笔记</Link></Space>
      <h1>{store.noteId ? '技术 Note 详情' : collectionTitle ?? (store.courseId ? '课程下的技术 Note' : '技术 Note')}</h1>
      {!store.noteId && store.collection === 'public' ? <><p className="muted">记录技术实践、代码片段与问题解决过程</p>
        <div className="note-discovery-toolbar"><NoteBrowseSearch />
          <Segmented aria-label="笔记排序" value={sort} options={[{ label: '最新', value: 'LATEST' }, { label: '热门', value: 'HOT' }]}
            onChange={value => void store.setSort(value as 'LATEST' | 'HOT')} /></div>
        {tag ? <Tag className="note-active-tag" color="blue" closable onClose={() => void store.clearTag()}>技术标签：{tag}</Tag> : null}</> : null}
    </header>
    {status === 'error' ? <Alert type="error" showIcon message={error} action={<Button onClick={() => void store.load()}>重试</Button>} />
      : status === 'loading' || status === 'idle' ? <div className="note-feed-skeleton" aria-label="正在加载笔记" role="status">
        {Array.from({ length: 6 }, (_, index) => <div key={index}><Skeleton active paragraph={{ rows: 3 + index % 3 }} /></div>)}
      </div>
      : store.noteId && note ? <article className="public-note-detail">
        <Link to={note.courseId ? `/notes/course/${encodeURIComponent(note.courseId)}` : '/notes'}>← 返回笔记列表</Link>
        <Typography.Title level={2}>{note.title}</Typography.Title>
        <div className="public-note-detail-meta">
          <UserIdentity userId={note.authorId} compact />
          <time dateTime={note.publishedAt ?? undefined}>发布于 {formatNoteDate(note.publishedAt)}</time>
          <Tag color={note.courseId ? 'blue' : 'default'}>{note.courseId ? '课程笔记' : '知识分享'}</Tag>
        </div>
        {note.tags?.length ? <div className="note-tag-list" aria-label="技术标签">
          {note.tags.map(item => <Tag key={item}>#{item}</Tag>)}
        </div> : null}
        {note.courseId ? <Link to={`/notes/course/${encodeURIComponent(note.courseId)}`}>浏览关联课程的全部公开笔记 →</Link> : null}
        <MarkdownDocument content={note.content} showToc />
        <NoteInteractionProvider><NoteInteractionPanel /></NoteInteractionProvider>
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
