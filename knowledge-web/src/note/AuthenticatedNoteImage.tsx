import { useEffect, useState } from 'react';
import { useAuth } from '../auth/AuthContext';

const INTERNAL_IMAGE = /^\/api\/learning\/note-images\/([0-9a-fA-F-]{36})$/;

export function noteImageId(src?: string) {
  return src?.match(INTERNAL_IMAGE)?.[1];
}

export function AuthenticatedNoteImage({ src, alt }: { src?: string; alt?: string }) {
  const { optionalAuthRequest } = useAuth();
  const [state, setState] = useState<{ url?: string; error?: string }>({});
  const imageId = noteImageId(src);

  useEffect(() => {
    let active = true;
    setState({});
    if (!imageId) {
      setState({ error: '已拦截非站内图片' }); return () => { active = false; };
    }
    void optionalAuthRequest<{ url: string }>(`/api/learning/note-images/${encodeURIComponent(imageId)}/access`)
      .then(result => { if (active) setState({ url: result.url }); })
      .catch(error => { if (active) setState({ error: error instanceof Error ? error.message : '图片加载失败' }); });
    return () => { active = false; };
  }, [optionalAuthRequest, imageId]);

  if (state.error) return <span className="note-image-error" role="img" aria-label={alt || '笔记图片'}>{state.error}</span>;
  if (!state.url) return <span className="note-image-loading" role="status">图片加载中…</span>;
  return <img src={state.url} alt={alt || '笔记图片'} loading="lazy" decoding="async" />;
}
