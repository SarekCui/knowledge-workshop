import { createContext, useContext, useEffect, useState, useSyncExternalStore, type ReactNode } from 'react';
import { useNavigate } from 'react-router';
import { useAuth } from '../auth/AuthContext';
import { PurchaseStore, type PurchaseView } from './PurchaseStore';

const PurchaseContext = createContext<PurchaseStore | null>(null);
export function PurchaseProvider({ view, children }: { view: PurchaseView; children: ReactNode }) {
  const { apiRequest } = useAuth();
  const [store] = useState(() => new PurchaseStore(view, apiRequest));
  useEffect(() => { void store.load(); return store.cancelPending; }, [store]);
  return <PurchaseContext.Provider value={store}>{children}</PurchaseContext.Provider>;
}
export function usePurchase() {
  const store = useContext(PurchaseContext);
  const navigate = useNavigate();
  if (!store) throw new Error('usePurchase 必须位于 PurchaseProvider 内');
  const state = useSyncExternalStore(store.subscribe, store.getSnapshot, store.getSnapshot);
  const join = async (id: string) => { const orderId = await store.join(id); if (orderId) navigate(`/orders/${encodeURIComponent(orderId)}`); };
  return { ...state, store, join };
}
