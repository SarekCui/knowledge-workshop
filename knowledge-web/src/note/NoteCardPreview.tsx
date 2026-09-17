import { AuthenticatedNoteImage } from './AuthenticatedNoteImage';
import { markdownExcerpt } from './markdown';

export function NoteCardPreview({ content }: { content: string }) {
  const withoutCode = content.replace(/(`{3,}|~{3,})[^\n]*\n[\s\S]*?\1/g, '');
  const image = withoutCode.match(/!\[([^\]]*)\]\((\/api\/learning\/note-images\/[0-9a-fA-F-]{36})\)/);
  const code = content.match(/(`{3,}|~{3,})([^\n]*)\n([\s\S]*?)\1/);
  if (image) return <div className="note-preview-image"><AuthenticatedNoteImage src={image[2]} alt={image[1]} /></div>;
  if (code) return <div className="note-preview-code">
    <span>{code[2].trim().slice(0, 24) || 'CODE'}</span>
    <pre><code>{code[3].trim().split('\n').slice(0, 8).join('\n')}</code></pre>
  </div>;
  return <p className="public-note-excerpt">{markdownExcerpt(content) || '暂无正文摘要'}</p>;
}
