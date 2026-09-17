import { describe, expect, it, vi } from 'vitest';
import { ApiError, request } from '../api';
import { AuthSession } from './AuthSession';

const pair = (accessToken = 'access-one') => ({ accessToken, expiresIn: 1800 });
function setup(handler: (path: string, token?: string, body?: unknown) => Promise<unknown>) {
  const transport = vi.fn(handler);
  return { transport, session: new AuthSession(transport as unknown as typeof request) };
}
describe('浏览器登录会话', () => {
  it('401重放保留PUT方法与原请求体', async () => {
    const transport = vi.fn(async (path: string, token?: string, body?: unknown) => {
      if (path.endsWith('/login')) return pair();
      if (path.endsWith('/refresh')) return pair('access-two');
      if (token === 'access-one') throw new ApiError(401, 'expired');
      return body;
    });
    const session = new AuthSession(transport as unknown as typeof request);
    await session.login({ username: 'user', password: 'password' });
    const body = { title: '名称', version: 3 };
    await session.apiRequest('/notes/one', body, { method: 'PUT' });
    expect(transport).toHaveBeenLastCalledWith('/notes/one', 'access-two', body, { method: 'PUT' });
  });
  it('启动通过Cookie刷新恢复登录，不需要持久化访问令牌', async () => {
    const { session, transport } = setup(async () => pair());
    await session.restore();
    expect(session.getSnapshot().status).toBe('authenticated');
    expect(transport).toHaveBeenCalledWith('/api/iam/web/auth/refresh', undefined, {}, { webAuth: true });
  });
  it('游客可请求公开资源，登录后同一请求自动携带令牌', async () => {
    const { session, transport } = setup(async path => path.endsWith('/login') ? pair() : { items: [] });
    await session.optionalAuthRequest('/api/learning/notes/public');
    expect(transport).toHaveBeenLastCalledWith('/api/learning/notes/public', undefined, undefined, undefined);
    await session.login({ username: 'user', password: 'password' });
    await session.optionalAuthRequest('/api/learning/notes/public');
    expect(transport).toHaveBeenLastCalledWith('/api/learning/notes/public', 'access-one', undefined, undefined);
  });
  it('并发401只刷新一次，重放保留原业务请求', async () => {
    let rotations = 0;
    const { session, transport } = setup(async (path, token, body) => {
      if (path.endsWith('/login')) return pair();
      if (path.endsWith('/refresh')) { rotations++; await Promise.resolve(); return pair('access-two'); }
      if (token === 'access-one') throw new ApiError(401, 'expired');
      return body;
    });
    await session.login({ username: 'user', password: 'password' });
    const event = { eventId: 'same-event', sequence: 1 };
    expect(await Promise.all([session.apiRequest('/business', event), session.apiRequest('/business', event)]))
      .toEqual([event, event]);
    expect(rotations).toBe(1);
    expect(transport.mock.calls.filter(call => call[0] === '/business')).toHaveLength(4);
  });
  it('503不透明重试业务写请求', async () => {
    const { session, transport } = setup(async path => {
      if (path.endsWith('/login')) return pair();
      throw new ApiError(503, 'offline');
    });
    await session.login({ username: 'user', password: 'password' });
    await expect(session.apiRequest('/business', {})).rejects.toMatchObject({ status: 503 });
    expect(transport.mock.calls.filter(call => call[0] === '/business')).toHaveLength(1);
  });
  it('刷新拒绝回到登录，依赖故障显示恢复失败而不是伪装退出', async () => {
    const denied = setup(async () => { throw new ApiError(401, 'expired'); });
    await denied.session.restore();
    expect(denied.session.getSnapshot().status).toBe('anonymous');
    const unavailable = setup(async () => { throw new ApiError(503, 'offline'); });
    await unavailable.session.restore();
    expect(unavailable.session.getSnapshot()).toMatchObject({ status: 'error', error: 'offline' });
  });
  it('刷新后仍401只重放一次并清除登录', async () => {
    const { session, transport } = setup(async path => {
      if (path.endsWith('/login') || path.endsWith('/refresh')) return pair();
      throw new ApiError(401, 'invalid');
    });
    await session.login({ username: 'user', password: 'password' });
    await expect(session.apiRequest('/business')).rejects.toMatchObject({ status: 401 });
    expect(session.getSnapshot().status).toBe('anonymous');
    expect(transport.mock.calls.filter(call => call[0] === '/business')).toHaveLength(2);
  });
  it('退出等待在途轮换，旧刷新响应不能恢复登录', async () => {
    let resolveRotation!: (value: unknown) => void;
    const calls: string[] = [];
    const { session } = setup(async path => {
      calls.push(path);
      if (path.endsWith('/login')) return pair();
      if (path.endsWith('/refresh')) return new Promise(resolve => { resolveRotation = resolve; });
      return null;
    });
    await session.login({ username: 'user', password: 'password' });
    const rotation = session.refresh().catch(() => {});
    const logout = session.logout();
    expect(calls.some(path => path.endsWith('/logout'))).toBe(false);
    resolveRotation(pair('access-two'));
    await Promise.all([rotation, logout]);
    expect(calls.at(-1)).toBe('/api/iam/web/auth/logout');
    expect(session.getSnapshot().status).toBe('anonymous');
    await expect(session.apiRequest('/business')).rejects.toMatchObject({ status: 401 });
  });
});
