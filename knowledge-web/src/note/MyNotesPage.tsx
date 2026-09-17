import { Link } from 'react-router';
import { NoteProvider } from './NoteContext';
import { NotePanel } from './NotePanel';

export function MyNotesPage() {
  return <section><header className="page-heading"><Link to="/notes">← 返回发现笔记</Link><h1>我的笔记</h1>
    <p className="muted">独立创作或关联课程，由你决定是否公开。</p></header>
    <NoteProvider courseId={null} paginated><NotePanel /></NoteProvider>
  </section>;
}
