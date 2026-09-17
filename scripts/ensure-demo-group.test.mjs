import { test } from 'node:test';
import assert from 'node:assert/strict';
import { ensureDemoGroup } from './ensure-demo-group.mjs';

test('课程缺失时停止，不创建团', async () => {
  const calls = [];
  await assert.rejects(ensureDemoGroup(async path => {
    calls.push(path);
    if (path.includes('/courses/')) throw Object.assign(new Error('课程不存在'), { status: 404 });
    return { courseId: 'course-java' };
  }), /课程不存在/);
  assert.equal(calls.length, 2);
});
test('复用可用团，不重复创建', async () => {
  const group = { id: 'available', status: 'FORMING' };
  const result = await ensureDemoGroup(async path => path.includes('/groups?') ? { items: [group] } : { courseId: 'course-java' });
  assert.equal(result, group);
});
test('已成团历史ID不重置，不重复占位', async () => {
  await assert.rejects(ensureDemoGroup(async path => {
    if (path.includes('/groups?')) return { items: [] };
    if (path.includes('/groups/')) return { id: 'group-demo-web-v1', status: 'FORMED' };
    return { courseId: 'course-java' };
  }), /不重置历史团/);
});
test('新团仅通过管理接口创建，不参团、不支付、不插入权益', async () => {
  const calls = [];
  const result = await ensureDemoGroup(async (path, body) => {
    calls.push({ path, body });
    if (path.includes('/groups?')) return { items: [] };
    if (path === '/api/marketing/groups/group-demo-web-v1') throw Object.assign(new Error('不存在'), { status: 404 });
    if (body) return { id: body.groupId, status: 'FORMING' };
    return { courseId: 'course-java', endTime: '2030-01-01T00:00:00Z' };
  });
  assert.equal(result.status, 'FORMING');
  assert.equal(calls.filter(call => call.body).length, 1);
  assert.equal(calls.at(-1).path, '/api/marketing/admin/activities/activity-demo/groups');
});
