import { describe, expect, it, vi } from 'vitest';
import { ApiError } from '../api';
import { paymentKey, PurchaseStore, type PurchaseTransport } from './PurchaseStore';
import type { Group, Order } from './types';

const group: Group = { id: 'group-1', activityId: 'activity-1', status: 'FORMING', targetCount: 2, confirmedCount: 0, expiresAt: '2099-01-01T00:00:00Z' };
const order: Order = { orderId: 'order-1', courseId: 'course-1', activityId: 'activity-1', groupId: 'group-1', amountCents: 100, status: 'PENDING_PAYMENT', paymentTradeNo: null, createdAt: '', paidAt: null };
const page = <T>(items: T[]) => ({ items, pageNo: 1, pageSize: 20, total: items.length });
const activity = { id: 'activity-1', courseId: 'course-1', priceCents: 100 };

describe('购买主线', () => {
  it('活动和订单只调用实际有界分页接口', async () => {
    const transport = vi.fn(async (_path: string) => page([]));
    await new PurchaseStore({ kind: 'activities' }, transport as PurchaseTransport).load(2);
    await new PurchaseStore({ kind: 'orders' }, transport as PurchaseTransport).load();
    expect(transport.mock.calls.map(call => call[0])).toEqual([
      '/api/marketing/activities?pageNo=2&pageSize=20', '/api/marketing/orders?pageNo=1&pageSize=20',
    ]);
  });
  it('参团重试复用团资源，没有额外随机去重键', async () => {
    let attempts = 0;
    const transport = vi.fn(async (path: string) => {
      if (path.endsWith('/join')) { if (++attempts === 1) throw new ApiError(503, '不可用'); return order; }
      if (path.includes('/groups?')) return page([group]);
      if (path.includes('/learning/')) return { title: '真实课程' };
      return activity;
    });
    const store = new PurchaseStore({ kind: 'activity', id: activity.id }, transport as PurchaseTransport);
    await store.load(); expect(await store.join(group.id)).toBeUndefined();
    expect(await store.join(group.id)).toBe(order.orderId);
    const calls = transport.mock.calls.filter(call => call[0].endsWith('/join'));
    expect(calls).toHaveLength(2); expect(calls[0]).toEqual(calls[1]);
  });
  it('重复点击参团只有一个在途写入，离开页面后不返回跳转目标', async () => {
    let finish!: (value: unknown) => void;
    const transport = vi.fn(async (path: string) => {
      if (path.endsWith('/join')) return new Promise(resolve => { finish = resolve; });
      if (path.includes('/groups?')) return page([group]);
      return activity;
    });
    const store = new PurchaseStore({ kind: 'activity', id: activity.id }, transport as PurchaseTransport);
    await store.load(); const first = store.join(group.id); await store.join(group.id);
    store.cancelPending(); finish(order); expect(await first).toBeUndefined();
    expect(transport.mock.calls.filter(call => call[0].endsWith('/join'))).toHaveLength(1);
  });
  it('支付超时不自动重试，手动重试使用同一流水号', async () => {
    const transport = vi.fn(async (path: string, _body?: unknown) => {
      if (path.endsWith('/pay')) throw new Error('超时');
      return path.includes('/groups/') ? group : order;
    });
    const store = new PurchaseStore({ kind: 'order', id: order.orderId }, transport as PurchaseTransport, () => 'same-uuid');
    await store.load(); await store.pay();
    expect(transport.mock.calls.filter(call => call[0].endsWith('/pay'))).toHaveLength(1);
    await store.pay();
    const writes = transport.mock.calls.filter(call => call[0].endsWith('/pay'));
    expect(writes[0]).toEqual(writes[1]); expect(writes[0][1]).toEqual({ paymentTradeNo: 'same-uuid' });
    expect(store.getSnapshot().order?.status).toBe('PENDING_PAYMENT');
  });
  it('支付前重新查询，已经支付不再发送写请求', async () => {
    let reads = 0;
    const transport = vi.fn(async (path: string) => path.includes('/groups/') ? group : ++reads === 1 ? order : { ...order, status: 'PAID' });
    const store = new PurchaseStore({ kind: 'order', id: order.orderId }, transport as PurchaseTransport);
    await store.load(); await store.pay();
    expect(store.getSnapshot().order?.status).toBe('PAID');
    expect(transport.mock.calls.some(call => call[0].endsWith('/pay'))).toBe(false);
  });
  it('支付成功后拼团查询故障，不伪装支付失败或成团成功', async () => {
    let groupReads = 0;
    const transport = vi.fn(async (path: string) => {
      if (path.includes('/groups/')) { if (++groupReads > 1) throw new ApiError(503, '故障'); return group; }
      return path.endsWith('/pay') ? { ...order, status: 'PAID' } : order;
    });
    const store = new PurchaseStore({ kind: 'order', id: order.orderId }, transport as PurchaseTransport, () => 'same-uuid');
    await store.load(); await store.pay();
    expect(store.getSnapshot()).toMatchObject({ order: { status: 'PAID' }, groups: [], error: '' });
    expect(store.getSnapshot().notice).toContain('模拟支付成功，但拼团状态刷新失败');
  });
  it('会话存储保留每张订单的UUID，重复调用不更换', () => {
    const entries = new Map<string, string>();
    const storage = { getItem: (key: string) => entries.get(key) ?? null, setItem: (key: string, value: string) => { entries.set(key, value); } };
    const first = paymentKey('order-1', storage);
    expect(paymentKey('order-1', storage)).toBe(first);
    expect(paymentKey('order-2', storage)).not.toBe(first);
    expect(first).toMatch(/^[a-f0-9-]{36}$/);
  });
});
