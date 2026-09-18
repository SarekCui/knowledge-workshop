import { Card, Tag, Typography } from 'antd';
import { Link } from 'react-router';
import { useNoteBrowse } from './NoteBrowseContext';
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
    {note.tags?.length ? <div className="note-tag-list" aria-label="技术标签">
      {note.tags.slice(0, 2).map(tag => store.collection === 'public'
        ? <button type="button" key={tag} onClick={() => void store.selectTag(tag)}>#{tag}</button>
        : <Tag key={tag}>#{tag}</Tag>)}
    </div> : null}
    <div className="note-card-footer">
      <UserIdentity userId={note.authorId} compact />
      <NoteReactionStat kind="like" count={note.likeCount ?? 0} active={note.liked} />
    </div>
  </Card>;
}
