export interface Page<T> { items: T[]; pageNo: number; pageSize: number; total: number }
export interface Activity { id: string; courseId: string; status: string; startTime: string; endTime: string; targetCount: number; maxJoinPerUser: number; priceCents: number }
export interface Group { id: string; activityId: string; status: string; targetCount: number; confirmedCount: number; expiresAt: string }
export interface Order { orderId: string; courseId: string; activityId: string; groupId: string; amountCents: number; status: string; paymentTradeNo: string | null; createdAt: string; paidAt: string | null }
export interface CourseDetail { id: string; title: string; summary: string }
export const money = (cents: number) => `¥${(cents / 100).toFixed(2)}`;
export const orderLabel = (status: string) => ({ PENDING_PAYMENT: '待支付', PAID: '已支付', CANCELLED: '已取消', CLOSED: '已关闭' }[status] ?? status);
