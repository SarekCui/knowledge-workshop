import { useNoteBrowse } from './NoteBrowseContext';
import { NoteCard } from './NoteCard';
import { useNoteMasonry } from './useNoteMasonry';

export function NoteFeed() {
  const { notes } = useNoteBrowse();
  const ref = useNoteMasonry(notes);
  return <div ref={ref} className="public-note-grid" aria-label="技术笔记列表">
    {notes.map(note => <NoteCard key={note.id} id={note.id} />)}
  </div>;
}
