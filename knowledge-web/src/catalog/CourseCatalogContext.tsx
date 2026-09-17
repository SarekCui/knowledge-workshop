import { createContext, useContext, useEffect, useState, useSyncExternalStore, type ReactNode } from 'react';
import { useAuth } from '../auth/AuthContext';
import { CourseCatalogStore, type CatalogView } from './CourseCatalogStore';

const CourseCatalogContext = createContext<CourseCatalogStore | null>(null);

export function CourseCatalogProvider({ view, children }: { view: CatalogView; children: ReactNode }) {
  const { optionalAuthRequest } = useAuth();
  const [store] = useState(() => new CourseCatalogStore(view, optionalAuthRequest));
  useEffect(() => { void store.load(); return store.cancelPending; }, [store]);
  return <CourseCatalogContext.Provider value={store}>{children}</CourseCatalogContext.Provider>;
}

export function useCourseCatalog() {
  const store = useContext(CourseCatalogContext);
  if (!store) throw new Error('useCourseCatalog 必须位于 CourseCatalogProvider 内');
  const state = useSyncExternalStore(store.subscribe, store.getSnapshot, store.getSnapshot);
  return { ...state, store };
}
