import { PictureOutlined } from '@ant-design/icons';
import { Button, Tooltip } from 'antd';
import { useRef, type ChangeEvent } from 'react';
import { useNotes } from './NoteContext';

export function NoteImageUploadButton() {
  const { draft, busy, store } = useNotes();
  const inputRef = useRef<HTMLInputElement>(null);
  if (!draft || draft.mode === 'rename') return null;

  const upload = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file) return;
    const textarea = document.getElementById('note-content') as HTMLTextAreaElement | null;
    const start = textarea?.selectionStart ?? draft.content.length;
    const end = textarea?.selectionEnd ?? start;
    const image = await store.uploadImage(file);
    if (!image) return;
    const current = store.getSnapshot().draft;
    if (!current) return;
    const alt = file.name.replace(/\.[^.]+$/, '').replace(/[\[\]]/g, '').slice(0, 80) || '技术图片';
    const markdown = `![${alt}](${image.markdownUrl})`;
    store.edit('content', `${current.content.slice(0, start)}${markdown}${current.content.slice(end)}`);
    requestAnimationFrame(() => {
      textarea?.focus(); textarea?.setSelectionRange(start + markdown.length, start + markdown.length);
    });
  };

  return <>
    <input ref={inputRef} className="visually-hidden" type="file" accept="image/jpeg,image/png"
      aria-label="选择 Note 图片" onChange={event => void upload(event)} />
    <Tooltip title="上传并插入站内图片">
      <Button size="small" aria-label="上传图片" icon={<PictureOutlined />} loading={busy}
        disabled={busy || draft.locked} onClick={() => inputRef.current?.click()}>图片</Button>
    </Tooltip>
  </>;
}
