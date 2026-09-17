import { Button, Space, Tooltip } from 'antd';
import { useNotes } from './NoteContext';
import { NoteImageUploadButton } from './NoteImageUploadButton';

interface SyntaxAction {
  label: string;
  description: string;
  prefix: string;
  suffix?: string;
  placeholder: string;
}

const actions: SyntaxAction[] = [
  { label: 'H2', description: '二级标题', prefix: '## ', placeholder: '章节标题' },
  { label: 'B', description: '粗体', prefix: '**', suffix: '**', placeholder: '重点内容' },
  { label: '`</>`', description: '行内代码', prefix: '`', suffix: '`', placeholder: 'code' },
  { label: '代码块', description: '技术代码块', prefix: '```java\n', suffix: '\n```', placeholder: '// 输入代码' },
  { label: '引用', description: '引用说明', prefix: '> ', placeholder: '引用内容' },
  { label: '列表', description: '无序列表', prefix: '- ', placeholder: '列表项' },
  { label: '链接', description: '外部链接', prefix: '[', suffix: '](https://)', placeholder: '链接文字' },
];

export function NoteMarkdownToolbar() {
  const { draft, busy, store } = useNotes();
  if (!draft || draft.mode === 'rename') return null;

  const insert = ({ prefix, suffix = '', placeholder }: SyntaxAction) => {
    const textarea = document.getElementById('note-content') as HTMLTextAreaElement | null;
    const start = textarea?.selectionStart ?? draft.content.length;
    const end = textarea?.selectionEnd ?? start;
    const selected = draft.content.slice(start, end) || placeholder;
    const insertion = `${prefix}${selected}${suffix}`;
    store.edit('content', `${draft.content.slice(0, start)}${insertion}${draft.content.slice(end)}`);
    requestAnimationFrame(() => {
      textarea?.focus();
      textarea?.setSelectionRange(start + prefix.length, start + prefix.length + selected.length);
    });
  };

  return <div className="markdown-toolbar" role="toolbar" aria-label="Markdown 编辑工具">
    <Space size={4} wrap>{actions.map(action => <Tooltip key={action.description} title={action.description}>
      <Button size="small" aria-label={action.description} disabled={busy || draft.locked}
        onClick={() => insert(action)}>{action.label}</Button>
    </Tooltip>)}<NoteImageUploadButton /></Space>
    <span>支持 Markdown · 代码块请标注语言</span>
  </div>;
}
