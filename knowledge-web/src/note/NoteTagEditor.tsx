import { Select } from 'antd';
import { useNotes } from './NoteContext';

export function NoteTagEditor() {
  const { draft, busy, store } = useNotes();
  if (!draft || draft.mode === 'rename') return null;
  return <div className="note-tag-editor">
    <label htmlFor="note-tags">技术标签</label>
    <Select id="note-tags" mode="tags" value={draft.tags} maxCount={5} tokenSeparators={[',', '，']}
      placeholder="输入 Java、Redis、Spring Boot 等标签，回车确认"
      disabled={busy || draft.locked} onChange={store.editTags} options={[]} />
    <span>最多 5 个，每个最多 20 个字符；标签可用于公开笔记的精确筛选。</span>
  </div>;
}
