import { createContext, useContext, useEffect, useState, useSyncExternalStore, type ReactNode } from 'react';
import { useParams } from 'react-router';
import { useAuth } from '../auth/AuthContext';
import { NoteBrowseStore, type NoteCollection } from './NoteBrowseStore';

const Context = createContext<NoteBrowseStore | null>(null);
export function NoteBrowseProvider({ children, collection = 'public' }: { children: ReactNode; collection?: NoteCollection }) {
  const { optionalAuthRequest } = useAuth();
  const { noteId, courseId } = useParams();
  const [store] = useState(() => new NoteBrowseStore(optionalAuthRequest, noteId, courseId, collection));
  useEffect(() => { void store.load(); return store.cancel; }, [store]);
  return <Context.Provider value={store}>{children}</Context.Provider>;
}
export function useNoteBrowse() {
  const store = useContext(Context);
  if (!store) throw new Error('useNoteBrowse 必须位于 NoteBrowseProvider 内');
  const state = useSyncExternalStore(store.subscribe, store.getSnapshot, store.getSnapshot);
  return { ...state, store };
}
