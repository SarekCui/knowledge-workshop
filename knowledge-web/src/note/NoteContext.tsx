import { createContext, useContext, useEffect, useState, useSyncExternalStore, type ReactNode } from 'react';
import { useAuth } from '../auth/AuthContext';
import { NoteStore } from './NoteStore';

const NoteContext = createContext<NoteStore | null>(null);
export function NoteProvider({ courseId, children, paginated = false }: { courseId: string | null; children: ReactNode; paginated?: boolean }) {
  const { apiRequest } = useAuth();
  const [store] = useState(() => new NoteStore(courseId, apiRequest, undefined, paginated));
  useEffect(() => { void store.load(); if (store.allowCourseSelection) void store.loadCourses(); return store.cancelPending; }, [store]);
  return <NoteContext.Provider value={store}>{children}</NoteContext.Provider>;
}
export function useNotes() {
  const store = useContext(NoteContext);
  if (!store) throw new Error('useNotes 必须位于 NoteProvider 内');
  const snapshot = useSyncExternalStore(store.subscribe, store.getSnapshot, store.getSnapshot);
  return { ...snapshot, store };
}
