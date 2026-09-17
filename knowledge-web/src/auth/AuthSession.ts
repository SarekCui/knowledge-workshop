import { ApiError, request, type RequestOptions } from '../api';

interface AccessToken { accessToken: string; expiresIn: number }
export interface AuthSnapshot {
  status: 'restoring' | 'authenticated' | 'anonymous' | 'error'; busy: boolean; error: string;
}
export class AuthSession {
  private accessToken?: string;
  private expiresAt = 0;
  private generation = 0;
  private flight?: Promise<void>;
  private listeners = new Set<() => void>();
  private snapshot: AuthSnapshot = { status: 'restoring', busy: false, error: '' };
  constructor(private transport: typeof request = request) {}
  getSnapshot = () => this.snapshot;
  subscribe = (listener: () => void) => { this.listeners.add(listener); return () => { this.listeners.delete(listener); }; };
  private publish(snapshot: AuthSnapshot) { this.snapshot = snapshot; this.listeners.forEach(listener => listener()); }
  private accept(pair: AccessToken, generation: number) {
    if (generation !== this.generation) throw new ApiError(401, '登录会话已变化');
    this.accessToken = pair.accessToken; this.expiresAt = Date.now() + pair.expiresIn * 1000;
    this.publish({ status: 'authenticated', busy: false, error: '' });
  }
  private clear(error = '') {
    this.accessToken = undefined; this.expiresAt = 0;
    this.publish({ status: 'anonymous', busy: false, error });
  }
  refresh = (): Promise<void> => {
    if (this.flight) return this.flight;
    const generation = this.generation;
    this.flight = this.transport<AccessToken>('/api/iam/web/auth/refresh', undefined, {}, { webAuth: true })
      .then(pair => this.accept(pair, generation)).catch(error => {
        if (generation === this.generation) {
          if (error instanceof ApiError && error.status === 401) this.clear(error.message === '请登录后继续' ? '' : '登录已过期，请重新登录');
          else this.publish({ status: 'error', busy: false, error: error instanceof Error ? error.message : '登录恢复失败' });
        }
        throw error;
      }).finally(() => { this.flight = undefined; });
    return this.flight;
  };
  restore = async () => {
    if (this.snapshot.status !== 'restoring' && this.snapshot.status !== 'error') return;
    this.publish({ status: 'restoring', busy: false, error: '' });
    try { await this.refresh(); } catch { /* Rejection and dependency failure are already visible in state. */ }
  };
  login = async (values: { username: string; password: string }) => {
    if (this.snapshot.busy) return;
    const generation = ++this.generation;
    this.publish({ status: 'anonymous', busy: true, error: '' });
    try {
      if (this.flight) await this.flight.catch(() => {});
      this.accept(await this.transport<AccessToken>('/api/iam/web/auth/login', undefined, values, { webAuth: true }), generation);
    } catch (error) { if (generation === this.generation) this.clear(error instanceof Error ? error.message : '登录失败'); }
  };
  logout = async () => {
    if (this.snapshot.busy) return;
    ++this.generation;
    this.clear(); this.publish({ status: 'anonymous', busy: true, error: '' });
    try {
      if (this.flight) await this.flight.catch(() => {});
      await this.transport('/api/iam/web/auth/logout', undefined, {}, { webAuth: true });
      this.clear();
    } catch (error) { this.clear(`退出请求未确认，请重试退出：${error instanceof Error ? error.message : '请求失败'}`); }
  };
  apiRequest = async <T>(path: string, body?: unknown, options?: RequestOptions): Promise<T> => {
    const generation = this.generation;
    if (!this.accessToken) throw new ApiError(401, '请登录后继续');
    if (Date.now() >= this.expiresAt - 30000) await this.refresh();
    if (generation !== this.generation || !this.accessToken) throw new ApiError(401, '登录会话已变化');
    const usedToken = this.accessToken;
    try {
      const data = await this.transport<T>(path, usedToken, body, options);
      if (generation !== this.generation) throw new ApiError(401, '登录会话已变化');
      return data;
    } catch (error) {
      if (!(error instanceof ApiError) || error.status !== 401 || generation !== this.generation) throw error;
      if (this.accessToken === usedToken) await this.refresh();
      if (generation !== this.generation || !this.accessToken) throw new ApiError(401, '请重新登录');
      let data: T;
      try { data = await this.transport<T>(path, this.accessToken, body, options); }
      catch (retryFailure) {
        if (retryFailure instanceof ApiError && retryFailure.status === 401 && generation === this.generation) {
          ++this.generation; this.clear('登录已失效，请重新登录');
        }
        throw retryFailure;
      }
      if (generation !== this.generation) throw new ApiError(401, '登录会话已变化');
      return data;
    }
  };
  optionalAuthRequest = async <T>(path: string, body?: unknown, options?: RequestOptions): Promise<T> => {
    if (this.snapshot.status === 'restoring') await this.refresh().catch(() => {});
    if (this.accessToken) return this.apiRequest<T>(path, body, options);
    return this.transport<T>(path, undefined, body, options);
  };
}
