import { Card, Space, Tag, Typography } from 'antd';
import { Link } from 'react-router';
import { useNoteBrowse } from './NoteBrowseContext';
import { formatNoteDate } from './formatNoteDate';
import { UserIdentity } from '../profile/UserIdentity';
import { NoteCardPreview } from './NoteCardPreview';
import { NoteReactionStat } from './NoteReaction';

export function NoteCard({ id }: { id: string }) {
  const { notes, store } = useNoteBrowse();
  const note = notes.find(item => item.id === id);
  if (!note) return null;
  return <Card className="public-note-card">
    <Link className="note-preview-link" to={`/notes/${encodeURIComponent(id)}`} aria-label={`阅读笔记：${note.title}`}>
      <NoteCardPreview content={note.content} />
    </Link>
    <Typography.Title level={3}><Link to={`/notes/${encodeURIComponent(id)}`}>{note.title}</Link></Typography.Title>
    <Tag color={note.courseId ? 'blue' : 'default'}>{note.courseId ? '课程笔记' : '知识分享'}</Tag>
    {note.tags?.length ? <div className="note-tag-list" aria-label="技术标签">
      {note.tags.slice(0, 3).map(tag => store.collection === 'public'
        ? <button type="button" key={tag} onClick={() => void store.selectTag(tag)}>#{tag}</button>
        : <Tag key={tag}>#{tag}</Tag>)}
    </div> : null}
    <div className="public-note-meta">
      <UserIdentity userId={note.authorId} compact />
      <time dateTime={note.publishedAt ?? undefined}>{formatNoteDate(note.publishedAt)}</time>
    </div>
    <Space className="note-card-stats" size="middle" aria-label="笔记互动数据">
      <NoteReactionStat kind="like" count={note.likeCount ?? 0} active={note.liked} />
      <NoteReactionStat kind="favorite" count={note.favoriteCount ?? 0} active={note.favorited} />
      <NoteReactionStat kind="comment" count={note.commentCount ?? 0} />
    </Space>
    {note.courseId ? <Link to={`/notes/course/${encodeURIComponent(note.courseId)}`}>浏览同课程笔记 →</Link> : null}
  </Card>;
}
