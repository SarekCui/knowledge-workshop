import { Alert, Input, Modal, Popconfirm, Button } from 'antd';
import { useEffect } from 'react';
import { useNotes } from './NoteContext';
import { NoteCourseSelect } from './NoteCourseSelect';
import { NoteEditorPreview } from './NoteEditorPreview';
import { NoteMarkdownToolbar } from './NoteMarkdownToolbar';
import { NoteTagEditor } from './NoteTagEditor';

export function NoteEditor() {
  const { draft, busy, error, store } = useNotes();
  const hasDraft = !!draft;
  useEffect(() => {
    if (!hasDraft) return;
    const warn = (event: BeforeUnloadEvent) => { event.preventDefault(); event.returnValue = ''; };
    window.addEventListener('beforeunload', warn);
    return () => window.removeEventListener('beforeunload', warn);
  }, [hasDraft]);
  if (!draft) return null;
  return <Modal open width={1120} title={draft.mode === 'create' ? '新建技术 Note' : draft.mode === 'rename' ? '重命名 Note' : '编辑技术 Note'}
    closable={false} maskClosable={false} keyboard={false}
    footer={<>
      <Popconfirm title="放弃未保存内容？" description={draft.locked ? '创建可能已成功，请先检查列表，避免重复创建。' : '关闭后未保存的草稿将丢失。'}
        onConfirm={store.close} disabled={busy}><Button disabled={busy}>关闭</Button></Popconfirm>
      <Button type="primary" loading={busy} disabled={draft.conflict} onClick={() => void store.save()}>{draft.locked ? '重试本次创建' : '保存'}</Button>
    </>}>
    <div className={draft.mode === 'rename' ? 'note-editor' : 'note-editor note-editor-layout'}>
    <div className="note-editor-fields">
    <NoteCourseSelect />
    {error ? <Alert type="error" message={error} showIcon /> : null}
    {draft.locked ? <Alert type="warning" message="本次创建内容已锁定，重试不会重复创建相同 Note。" /> : null}
    <label htmlFor="note-title">名称</label>
    <Input id="note-title" value={draft.title} maxLength={100} showCount disabled={busy || draft.locked}
      onChange={event => store.edit('title', event.target.value)} />
    <NoteTagEditor />
    {draft.mode !== 'rename' ? <>
      <div className="note-content-label"><label htmlFor="note-content">Markdown 正文</label><span>最多 20,000 字符</span></div>
      <NoteMarkdownToolbar />
      <Input.TextArea id="note-content" value={draft.content} maxLength={20000} showCount autoSize={{ minRows: 8, maxRows: 16 }}
        disabled={busy || draft.locked} onChange={event => store.edit('content', event.target.value)} />
    </> : null}
    </div>
    <NoteEditorPreview />
    </div>
  </Modal>;
}
