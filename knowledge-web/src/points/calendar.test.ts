import { expect, it } from 'vitest';
import { monthCells, moveMonth } from './calendar';
import { PointsStore } from './PointsStore';
import { vi } from 'vitest';

it('闰年、周一对齐与跨年月份', () => {
  const february = monthCells('2024-02');
  expect(february.filter(Boolean)).toHaveLength(29);
  expect(february[3]).toBe('2024-02-01');
  expect(february.length % 7).toBe(0);
  expect(moveMonth('2026-01', -1)).toBe('2025-12');
  expect(moveMonth('2026-12', 1)).toBe('2027-01');
});
it('切月忽略旧响应，失败不保留上月轨迹', async () => {
  let resolve!: (value: unknown) => void;
  const request = vi.fn().mockImplementationOnce(() => new Promise(done => { resolve = done; }))
    .mockResolvedValueOnce({ month: '2026-08', today: '2026-09-14', signedDates: ['2026-08-01'] })
    .mockRejectedValueOnce(new Error('查询失败'));
  const store = new PointsStore(request);
  const old = store.loadMonth('2026-09'); await store.loadMonth('2026-08');
  resolve({ month: '2026-09', today: '2026-09-14', signedDates: [] }); await old;
  expect(store.getSnapshot().calendar?.signedDates).toEqual(['2026-08-01']);
  await store.loadMonth('2026-07');
  expect(store.getSnapshot().calendar).toBeUndefined();
  expect(store.getSnapshot().calendarError).toBe('查询失败');
});
