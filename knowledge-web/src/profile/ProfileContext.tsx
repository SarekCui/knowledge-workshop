import { createContext, useContext, useEffect, useState, useSyncExternalStore, type ReactNode } from 'react';
import { useAuth } from '../auth/AuthContext';
import { ProfileStore } from './ProfileStore';

const Context = createContext<ProfileStore | null>(null);

export function ProfileProvider({ children }: { children: ReactNode }) {
  const { status, apiRequest } = useAuth();
  const [store] = useState(() => new ProfileStore(apiRequest));
  useEffect(() => {
    if (status === 'authenticated') void store.load();
    else store.reset();
  }, [status, store]);
  return <Context.Provider value={store}>{children}</Context.Provider>;
}

export function useProfile() {
  const store = useContext(Context);
  if (!store) throw new Error('useProfile 必须位于 ProfileProvider 内');
  const state = useSyncExternalStore(store.subscribe, store.getSnapshot, store.getSnapshot);
  return { ...state, store };
}
