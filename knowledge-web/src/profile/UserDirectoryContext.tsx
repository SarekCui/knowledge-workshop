import { createContext, useContext, useEffect, useState, useSyncExternalStore, type ReactNode } from 'react';
import { useAuth } from '../auth/AuthContext';
import { useProfile } from './ProfileContext';
import { UserDirectoryStore } from './UserDirectoryStore';

const Context = createContext<UserDirectoryStore | null>(null);

export function UserDirectoryProvider({ children }: { children: ReactNode }) {
  const { status, optionalAuthRequest } = useAuth();
  const { profile } = useProfile();
  const [store] = useState(() => new UserDirectoryStore(optionalAuthRequest));
  useEffect(() => { if (status !== 'authenticated') store.reset(); }, [status, store]);
  useEffect(() => {
    if (profile) store.upsert({
      userId: profile.userId,
      nickname: profile.nickname,
      avatarUrl: profile.avatarUrl,
    });
  }, [profile, store]);
  return <Context.Provider value={store}>{children}</Context.Provider>;
}

export function useUserDirectory() {
  const store = useContext(Context);
  if (!store) throw new Error('useUserDirectory 必须位于 UserDirectoryProvider 内');
  const state = useSyncExternalStore(store.subscribe, store.getSnapshot, store.getSnapshot);
  return { ...state, store };
}
