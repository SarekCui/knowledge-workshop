import { createContext, useContext, useEffect, useMemo, useState, useSyncExternalStore, type ReactNode } from 'react';
import { useParams } from 'react-router';
import { useAuth } from '../auth/AuthContext';
import { NoteInteractionStore } from './NoteInteractionStore';
import type { NoteTransport } from './NoteStore';

const Context = createContext<NoteInteractionStore | null>(null);

export function NoteInteractionProvider({ children }: { children: ReactNode }) {
  const { noteId } = useParams();
  const { apiRequest, optionalAuthRequest } = useAuth();
  if (!noteId) throw new Error('笔记互动必须位于详情路由内');
  const transport = useMemo<NoteTransport>(() => (path, body, options) => {
    const readOnly = body === undefined && (options?.method === undefined || options.method === 'GET');
    return readOnly ? optionalAuthRequest(path, body, options) : apiRequest(path, body, options);
  }, [apiRequest, optionalAuthRequest]);
  const [store] = useState(() => new NoteInteractionStore(noteId, transport, undefined, optionalAuthRequest));
  useEffect(() => { void store.load(); return store.cancel; }, [store]);
  return <Context.Provider value={store}>{children}</Context.Provider>;
}

export function useNoteInteraction() {
  const store = useContext(Context);
  if (!store) throw new Error('useNoteInteraction 必须位于 NoteInteractionProvider 内');
  const state = useSyncExternalStore(store.subscribe, store.getSnapshot, store.getSnapshot);
  return { ...state, store };
}
