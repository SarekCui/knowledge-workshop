import { createContext, useContext, useEffect, useState, useSyncExternalStore, type ReactNode } from 'react';
import { AuthSession } from './AuthSession';

const AuthContext = createContext<AuthSession | null>(null);
export function AuthProvider({ children }: { children: ReactNode }) {
  const [session] = useState(() => new AuthSession());
  useEffect(() => { void session.restore(); }, [session]);
  return <AuthContext.Provider value={session}>{children}</AuthContext.Provider>;
}
export function useAuth() {
  const session = useContext(AuthContext);
  if (!session) throw new Error('useAuth 必须位于 AuthProvider 内');
  const snapshot = useSyncExternalStore(session.subscribe, session.getSnapshot, session.getSnapshot);
  return { ...snapshot, login: session.login, logout: session.logout, restore: session.restore,
    apiRequest: session.apiRequest, agentStreamRequest: session.agentStreamRequest,
    optionalAuthRequest: session.optionalAuthRequest };
}
