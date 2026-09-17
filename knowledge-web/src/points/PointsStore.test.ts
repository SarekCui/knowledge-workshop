import { expect, it, vi } from 'vitest';
import { PointsStore, utcSeason } from './PointsStore';

const result = { userId: 'user-1', signDate: '2026-09-14', continuousDays: 2, rewardPoints: 10, duplicated: false };
it('自然季度按 UTC 而不是浏览器本地日期计算', () => {
  expect(utcSeason(new Date('2026-10-01T00:30:00+08:00'))).toBe('2026-Q3');
  expect(utcSeason(new Date('2026-10-01T00:00:00Z'))).toBe('2026-Q4');
});
it('签到使用 POST，成功不会乐观累加积分', async () => {
  const request = vi.fn().mockResolvedValueOnce(result).mockResolvedValueOnce([]);
  const store = new PointsStore(request, () => new Date('2026-09-14T00:00:00Z'));
  await store.sign();
  expect(request.mock.calls[0]).toEqual(['/api/points/sign-ins', undefined, { method: 'POST' }]);
  expect(store.getSnapshot().signIn).toEqual(result);
  expect(store.getSnapshot().ranking).toEqual([]);
});
it('签到成功但榜单失败不会改成签到失败', async () => {
  const request = vi.fn().mockResolvedValueOnce({ ...result, duplicated: true }).mockRejectedValueOnce(new Error('榜单不可用'));
  const store = new PointsStore(request);
  await store.sign();
  expect(store.getSnapshot().signIn?.duplicated).toBe(true);
  expect(store.getSnapshot().signError).toBe('');
  expect(store.getSnapshot().rankingError).toBe('榜单不可用');
});
it('未知签到结果允许人工确认重试，不重复并发提交', async () => {
  let reject!: (error: Error) => void;
  const request = vi.fn().mockImplementationOnce(() => new Promise((_, fail) => { reject = fail; }))
    .mockResolvedValueOnce({ ...result, duplicated: true }).mockResolvedValueOnce([]);
  const store = new PointsStore(request);
  const pending = store.sign();
  await store.sign();
  expect(request).toHaveBeenCalledTimes(1);
  reject(new Error('超时')); await pending;
  expect(store.getSnapshot().signError).toContain('结果未确认');
  await store.sign();
  expect(store.getSnapshot().signIn?.duplicated).toBe(true);
});
it('忽略旧榜单查询和离开页面后的签到响应', async () => {
  let old!: (items: unknown) => void;
  const request = vi.fn().mockImplementationOnce(() => new Promise(resolve => { old = resolve; }))
    .mockResolvedValueOnce([{ userId: 'new', score: 20, rank: 1 }]);
  const store = new PointsStore(request);
  const first = store.refresh(); await store.refresh(); old([]); await first;
  expect(store.getSnapshot().ranking[0]?.userId).toBe('new');
  let finish!: (value: unknown) => void;
  request.mockImplementationOnce(() => new Promise(resolve => { finish = resolve; }));
  const sign = store.sign(); store.cancelPending(); finish(result); await sign;
  expect(store.getSnapshot().signIn).toBeUndefined();
  expect(request).toHaveBeenCalledTimes(3);
});
