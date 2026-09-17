import { mkdir, readFile, rename, writeFile } from 'node:fs/promises';

// Operator test data only; never insert entitlements or modify frontend source data.
const base = 'http://127.0.0.1:8080';
const file = new URL('../data/local-oceans-data.json', import.meta.url);
const password = process.env.LOCAL_TEST_PASSWORD ?? 'Knowledge@123';
const videoUrl = 'http://127.0.0.1:8090/oceans.mp4';
let state;
try { state = JSON.parse(await readFile(file, 'utf8')); }
catch (error) {
  if (error.code !== 'ENOENT') throw error;
  const now = Date.now();
  state = { startTime: new Date(now - 60000).toISOString(),
    endTime: new Date(now + 7 * 86400000).toISOString(),
    expiresAt: new Date(now + 6 * 86400000).toISOString() };
}
async function save() {
  await mkdir(new URL('../data/', import.meta.url), { recursive: true });
  const temporary = new URL(`${file.href}.tmp`);
  await writeFile(temporary, JSON.stringify(state, null, 2));
  await rename(temporary, file);
}
async function api(path, token, body, method = body === undefined ? 'GET' : 'POST') {
  const response = await fetch(`${base}${path}`, {
    method, signal: AbortSignal.timeout(15000),
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    ...(body === undefined ? {} : { body: JSON.stringify(body) }),
  });
  const result = await response.json();
  if (!response.ok) throw new Error(`${method} ${path}: HTTP ${response.status} ${result.message}`);
  return result.data;
}
async function login(username) {
  return (await api('/api/iam/auth/login', undefined, { username, password })).accessToken;
}
const head = await fetch(videoUrl, { method: 'HEAD', signal: AbortSignal.timeout(5000) });
if (!head.ok || head.headers.get('accept-ranges') !== 'bytes') throw new Error('请先启动指定视频的Range服务');
await save();
const admin = await login('local-test-admin');
const demo = await login('demo');
const peer = await login('local-test-peer');
if (!state.course) {
  state.course = await api('/api/learning/admin/courses', admin, {
    title: 'Oceans 视频学习测试', summary: '用户提供的本地视频，用于暂停、拖动和断点恢复验收', priceCents: 100,
  });
  await save();
}
if (!state.chapter) {
  state.chapter = await api(`/api/learning/admin/courses/${state.course.id}/chapters`, admin, {
    title: 'Oceans · 断点续播测试', sortOrder: 1, videoId: 'video-oceans-local-v1',
    videoUrl, videoDurationMs: 46613, videoVersion: 1, status: 'PUBLISHED',
  });
  await save();
}
if (state.course.status !== 'PUBLISHED') {
  state.course = await api(`/api/learning/admin/courses/${state.course.id}/status`, admin,
    { status: 'PUBLISHED', version: state.course.version }, 'PATCH');
  await save();
}
if (!state.activity) {
  state.activity = await api('/api/marketing/admin/activities', admin, {
    activityId: 'activity-oceans-local', courseId: state.course.id,
    startTime: state.startTime, endTime: state.endTime, targetCount: 2, maxJoinPerUser: 2, priceCents: 100,
  });
  await save();
}
if (state.activity.status !== 'ACTIVE') {
  state.activity = await api(`/api/marketing/admin/activities/${state.activity.id}/status`, admin,
    { status: 'ACTIVE', version: state.activity.version }, 'PATCH');
  await save();
}
if (!state.group) {
  state.group = await api(`/api/marketing/admin/activities/${state.activity.id}/groups`, admin, {
    groupId: 'group-oceans-local', ownerUserId: 'user-demo', expiresAt: state.expiresAt,
  });
  await save();
}
for (const [key, token] of [['demoOrder', demo], ['peerOrder', peer]]) {
  if (!state[key]) {
    state[key] = await api(`/api/marketing/groups/${state.group.id}/join`, token, {});
    await save();
  }
  state[key] = await api(`/api/marketing/orders/${state[key].orderId}/pay`, token,
    { paymentTradeNo: `local-oceans-${state[key].orderId}` });
  await save();
}
for (let attempt = 0; attempt < 20; attempt++) {
  const [mine, theirs] = await Promise.all([
    api('/api/learning/my-courses?pageNo=1&pageSize=100', demo),
    api('/api/learning/my-courses?pageNo=1&pageSize=100', peer),
  ]);
  if (mine.items.some(item => item.courseId === state.course.id)
    && theirs.items.some(item => item.courseId === state.course.id)) {
    console.log(`测试数据就绪: courseId=${state.course.id}, videoId=${state.chapter.videoId}, groupId=${state.group.id}`);
    console.log('通过真实参团、模拟支付与MQ消费发放权益；前端使用demo账号查看。');
    process.exit(0);
  }
  await new Promise(resolve => setTimeout(resolve, 1000));
}
throw new Error('成团后权益尚未收敛，请检查通知任务/消费者；不会直接插入权益');
