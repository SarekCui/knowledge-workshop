import type { RequestOptions } from '../api';
import type { Activity, CourseDetail, Group, Order, Page } from './types';

export type PurchaseTransport = <T>(path: string, body?: unknown, options?: RequestOptions) => Promise<T>;
export type PurchaseView = { kind: 'activities' | 'orders' } | { kind: 'activity' | 'order'; id: string };
interface Snapshot {
  status: 'idle' | 'loading' | 'success' | 'error'; busy: boolean; error: string; notice: string;
  activities: Activity[]; groups: Group[]; orders: Order[]; activity?: Activity; order?: Order;
  course?: CourseDetail; courseError: string; page: number; total: number;
}
export function paymentKey(orderId: string, storage: Pick<Storage, 'getItem' | 'setItem'> = sessionStorage): string {
  const key = `kw:mock-payment:${orderId}`;
  const existing = storage.getItem(key);
  if (existing) return existing;
  const value = crypto.randomUUID(); storage.setItem(key, value); return value;
}
export class PurchaseStore {
  private snapshot: Snapshot = { status: 'idle', busy: false, error: '', notice: '', activities: [], groups: [], orders: [], courseError: '', page: 1, total: 0 };
  private listeners = new Set<() => void>();
  private generation = 0;
  constructor(private view: PurchaseView, private transport: PurchaseTransport,
    private getPaymentKey: (id: string) => string = paymentKey) {}
  getSnapshot = () => this.snapshot;
  subscribe = (listener: () => void) => { this.listeners.add(listener); return () => { this.listeners.delete(listener); }; };
  private publish(change: Partial<Snapshot>) { this.snapshot = { ...this.snapshot, ...change }; this.listeners.forEach(listener => listener()); }
  cancelPending = () => { ++this.generation; };
  load = async (page = this.snapshot.page) => {
    if (this.snapshot.busy) return;
    const generation = ++this.generation;
    this.publish({ status: 'loading', error: '' });
    try {
      let change: Partial<Snapshot>;
      const pagination = `pageNo=${page}&pageSize=20`;
      if (this.view.kind === 'activities') {
        const result = await this.transport<Page<Activity>>(`/api/marketing/activities?${pagination}`);
        change = { activities: result.items, total: result.total, page: result.pageNo };
      } else if (this.view.kind === 'orders') {
        const result = await this.transport<Page<Order>>(`/api/marketing/orders?${pagination}`);
        change = { orders: result.items, total: result.total, page: result.pageNo };
      } else if (this.view.kind === 'activity') {
        const base = `/api/marketing/activities/${encodeURIComponent(this.view.id)}`;
        const [activity, groups] = await Promise.all([this.transport<Activity>(base), this.transport<Page<Group>>(`${base}/groups?${pagination}`)]);
        let course: CourseDetail | undefined;
        let courseError = '';
        try { course = await this.transport<CourseDetail>(`/api/learning/courses/${encodeURIComponent(activity.courseId)}`); }
        catch (error) { courseError = this.message(error); }
        change = { activity, groups: groups.items, total: groups.total, page: groups.pageNo, course, courseError };
      } else if (this.view.kind === 'order') {
        const order = await this.transport<Order>(`/api/marketing/orders/${encodeURIComponent(this.view.id)}`);
        const group = await this.transport<Group>(`/api/marketing/groups/${encodeURIComponent(order.groupId)}`);
        change = { order, groups: [group] };
      } else { throw new Error('不支持的购买页面'); }
      if (generation === this.generation) this.publish({ ...change, status: 'success' });
    } catch (error) {
      if (generation === this.generation) this.publish({ status: 'error', error: this.message(error) });
    }
  };
  join = async (groupId: string): Promise<string | undefined> => {
    if (this.snapshot.busy || this.snapshot.status !== 'success' || this.view.kind !== 'activity') return;
    const group = this.snapshot.groups.find(item => item.id === groupId);
    if (!group || group.status !== 'FORMING' || Date.parse(group.expiresAt) <= Date.now()) return;
    const generation = ++this.generation;
    this.publish({ busy: true, error: '', notice: '' });
    try {
      const order = await this.transport<Order>(`/api/marketing/groups/${encodeURIComponent(groupId)}/join`, {});
      if (generation === this.generation) return order.orderId;
    } catch (error) { if (generation === this.generation) this.publish({ error: `参团未确认，请重试同一团或查看我的订单：${this.message(error)}` }); }
    finally { if (generation === this.generation) this.publish({ busy: false }); }
  };
  pay = async () => {
    if (this.snapshot.busy || this.snapshot.status !== 'success' || !this.snapshot.order) return;
    const id = this.snapshot.order.orderId;
    const generation = ++this.generation;
    this.publish({ busy: true, error: '', notice: '' });
    try {
      const latest = await this.transport<Order>(`/api/marketing/orders/${encodeURIComponent(id)}`);
      if (generation !== this.generation) return;
      this.publish({ order: latest });
      if (latest.status !== 'PENDING_PAYMENT') {
        this.publish({ notice: '订单状态已更新，请以查询结果为准。' }); return;
      }
      const key = this.getPaymentKey(id);
      const order = await this.transport<Order>(`/api/marketing/orders/${encodeURIComponent(id)}/pay`, { paymentTradeNo: key });
      if (generation !== this.generation) return;
      this.publish({ order, notice: '模拟支付成功；成团后课程权益由后台异步发放，请在我的学习刷新查看。' });
      try {
        const group = await this.transport<Group>(`/api/marketing/groups/${encodeURIComponent(order.groupId)}`);
        if (generation === this.generation) this.publish({ groups: [group] });
      } catch (error) {
        if (generation === this.generation) this.publish({ groups: [], notice: `模拟支付成功，但拼团状态刷新失败，请刷新查询：${this.message(error)}` });
      }
    } catch (error) {
      if (generation === this.generation) this.publish({ error: `模拟支付结果未确认，请刷新订单；若仍待支付，可复用原流水重试：${this.message(error)}` });
    } finally { if (generation === this.generation) this.publish({ busy: false }); }
  };
  private message(error: unknown) { return error instanceof Error ? error.message : '请求失败'; }
}
