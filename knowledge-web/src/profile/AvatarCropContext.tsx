import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';

interface AvatarCropSession {
  sourceUrl: string | null;
  filename: string;
  error: string;
  open: (file: File) => void;
  close: () => void;
}

const Context = createContext<AvatarCropSession | null>(null);

export function AvatarCropProvider({ children }: { children: ReactNode }) {
  const [source, setSource] = useState<{ url: string; filename: string } | null>(null);
  const [error, setError] = useState('');
  useEffect(() => () => { if (source) URL.revokeObjectURL(source.url); }, [source]);
  const value = useMemo<AvatarCropSession>(() => ({
    sourceUrl: source?.url ?? null,
    filename: source?.filename ?? 'avatar.jpg',
    error,
    open: file => {
      if (!['image/jpeg', 'image/png'].includes(file.type)) {
        setError('请选择 JPEG 或 PNG 图片');
        return;
      }
      if (file.size > 2 * 1024 * 1024) {
        setError('原始头像文件不能超过 2MB');
        return;
      }
      setError('');
      setSource(current => {
        if (current) URL.revokeObjectURL(current.url);
        return { url: URL.createObjectURL(file), filename: file.name };
      });
    },
    close: () => setSource(current => {
      if (current) URL.revokeObjectURL(current.url);
      return null;
    }),
  }), [source, error]);
  return <Context.Provider value={value}>{children}</Context.Provider>;
}

export function useAvatarCrop() {
  const session = useContext(Context);
  if (!session) throw new Error('useAvatarCrop 必须位于 AvatarCropProvider 内');
  return session;
}
