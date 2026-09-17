import { expect, it, vi } from 'vitest';
import { UserDirectoryStore } from './UserDirectoryStore';

it('批量去重查询作者并缓存公开资料', async () => {
  const transport = vi.fn().mockResolvedValue([
    { userId: 'u1', nickname: '甲', avatarUrl: null },
    { userId: 'u2', nickname: '乙', avatarUrl: 'signed' },
  ]);
  const store = new UserDirectoryStore(transport);
  await store.ensure(['u1', 'u2', 'u1']);
  await store.ensure(['u1']);
  expect(transport).toHaveBeenCalledTimes(1);
  expect(transport.mock.calls[0][0]).toContain('userIds=u1');
  expect(store.getSnapshot().profiles.u2.nickname).toBe('乙');
});

it('当前用户更新资料后立即刷新公开作者缓存', () => {
  const store = new UserDirectoryStore(vi.fn());
  store.upsert({ userId: 'u1', nickname: '新昵称', avatarUrl: 'new-avatar' });
  expect(store.getSnapshot().profiles.u1).toEqual({
    userId: 'u1', nickname: '新昵称', avatarUrl: 'new-avatar',
  });
});
