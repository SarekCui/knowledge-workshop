import { Empty, Typography } from 'antd';
import { useDeferredValue } from 'react';
import { useNotes } from './NoteContext';
import { MarkdownDocument } from './MarkdownDocument';

export function NoteEditorPreview() {
  const { draft } = useNotes();
  const previewContent = useDeferredValue(draft?.content ?? '');
  if (!draft || draft.mode === 'rename') return null;
  return <section className="note-editor-preview" aria-label="笔记内容预览">
    <span className="eyebrow">实时预览</span>
    {draft.title.trim() || draft.content.trim() ? <article>
      <Typography.Title level={3}>{draft.title.trim() || '未命名笔记'}</Typography.Title>
      <MarkdownDocument content={previewContent || '正文尚未填写'} />
    </article> : <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="输入标题和正文后在这里预览" />}
  </section>;
}
