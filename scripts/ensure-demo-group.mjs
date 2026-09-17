import { pathToFileURL } from 'node:url';

export async function ensureDemoGroup(api, groupId = 'group-demo-web-v1') {
  const activity = await api('/api/marketing/activities/activity-demo');
  if (activity.courseId !== 'course-java') throw new Error('活动课程关联已改变，请人工核对，不自动覆盖');
  await api('/api/learning/courses/course-java');
  const groups = await api('/api/marketing/activities/activity-demo/groups?pageNo=1&pageSize=20');
  if (groups.items.length) return groups.items[0];
  let existing;
  try { existing = await api(`/api/marketing/groups/${encodeURIComponent(groupId)}`); }
  catch (error) { if (error.status !== 404) throw error; }
  if (existing) throw new Error('测试团已存在但不可加入；不重置历史团。如需新团请指定新的 LOCAL_TEST_GROUP_ID');
  return api('/api/marketing/admin/activities/activity-demo/groups', {
    groupId, ownerUserId: 'user-demo',
    expiresAt: new Date(Math.min(Date.now() + 7 * 86400000, Date.parse(activity.endTime))).toISOString(),
  });
}

async function main() {
  const base = 'http://127.0.0.1:8080';
  let token;
  async function api(path, body) {
    const response = await fetch(`${base}${path}`, {
      method: body === undefined ? 'GET' : 'POST', signal: AbortSignal.timeout(15000),
      headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
      ...(body === undefined ? {} : { body: JSON.stringify(body) }),
    });
    const result = await response.json();
    if (!response.ok) throw Object.assign(new Error(`${path}: ${result.message}`), { status: response.status });
    return result.data;
  }
  const login = await api('/api/iam/auth/login', { username: 'local-test-admin', password: process.env.LOCAL_TEST_PASSWORD ?? 'Knowledge@123' });
  token = login.accessToken;
  const group = await ensureDemoGroup(api, process.env.LOCAL_TEST_GROUP_ID);
  console.log(`测试团就绪: groupId=${group.id}, status=${group.status}`);
  console.log('不自动参团或支付，请从前端操作。');
}
if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  main().catch(error => { console.error(`Error: ${error.message}`); process.exitCode = 1; });
}
