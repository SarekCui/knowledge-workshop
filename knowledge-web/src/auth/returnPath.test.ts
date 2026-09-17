import { expect, it } from 'vitest';
import { returnPath } from './returnPath';
it('登录后只返回已知业务路径，不允许外部跳转', () => {
  for (const path of ['/account', '/courses', '/courses/course-1', '/points', '/activities', '/activities/activity-1', '/orders', '/orders/order-1', '/learning/courses/course-1']) expect(returnPath(path)).toBe(path);
  for (const path of ['https://example.com', '//example.com', '/admin', '/orders/one/unknown', undefined]) expect(returnPath(path)).toBe('/learning');
});
