import { renderToStaticMarkup } from 'react-dom/server';
import { MemoryRouter } from 'react-router';
import { beforeEach, expect, it, vi } from 'vitest';
import { OrderPage } from './OrderPage';
import type { Order } from './types';

const state = vi.hoisted(() => ({ order: { orderId: 'order-1', courseId: 'course-1', groupId: 'group-1',
  activityId: 'activity-1', status: 'PENDING_PAYMENT', amountCents: 125, paymentTradeNo: null, createdAt: '', paidAt: null } as Order,
  groups: [], status: 'success', error: '', notice: '', busy: false, store: { load: () => {}, pay: () => {} } }));
vi.mock('./PurchaseContext', () => ({ usePurchase: () => state }));
const render = () => renderToStaticMarkup(<MemoryRouter><OrderPage /></MemoryRouter>);
beforeEach(() => { state.status = 'success'; state.order.status = 'PENDING_PAYMENT'; state.error = ''; });
it('待支付订单显示真实金额与模拟支付入口，不假造拼团状态', () => {
  const html = render();
  expect(html).toContain('¥1.25'); expect(html).toContain('模拟支付</span>');
  expect(html).toContain('状态未知，请刷新查询');
});
it('已支付订单不再提供支付按钮', () => {
  state.order.status = 'PAID';
  const html = render();
  expect(html).toContain('已支付'); expect(html).not.toContain('模拟支付</span>');
});
it('查询错误显示失败，不展示旧订单的支付入口', () => {
  state.status = 'error'; state.error = '订单查询失败';
  const html = render();
  expect(html).toContain('订单查询失败'); expect(html).not.toContain('模拟支付</span>');
});
