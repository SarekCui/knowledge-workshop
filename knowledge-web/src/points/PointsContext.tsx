import { createContext, useContext, useEffect, useState, useSyncExternalStore, type ReactNode } from 'react';
import { useAuth } from '../auth/AuthContext';
import { PointsStore } from './PointsStore';

const PointsContext = createContext<PointsStore | null>(null);
export function PointsProvider({ children }: { children: ReactNode }) {
  const { apiRequest } = useAuth();
  const [store] = useState(() => new PointsStore(apiRequest));
  useEffect(() => { void store.refresh(); void store.loadMonth(); return store.cancelPending; }, [store]);
  return <PointsContext.Provider value={store}>{children}</PointsContext.Provider>;
}
export function usePoints() {
  const store = useContext(PointsContext);
  if (!store) throw new Error('usePoints 必须位于 PointsProvider 内');
  const snapshot = useSyncExternalStore(store.subscribe, store.getSnapshot, store.getSnapshot);
  return { ...snapshot, sign: store.sign, refresh: store.refresh, loadMonth: store.loadMonth };
}
