// Local operator fixture: creates notes only through authenticated business APIs.
const base = 'http://127.0.0.1:8080';
const password = process.env.LOCAL_TEST_PASSWORD ?? 'Knowledge@123';

const fixtures = [
  {
    username: 'demo',
    clientRequestId: '9a908571-e394-4e85-8a50-617e8640dd01',
    title: 'Redis 缓存不是事实源：视频进度如何安全落库',
    content: '断点续播不能只把 position 写进 Redis。\n\n本项目将 MySQL 作为进度事实源，Redis 保存高频快照；查询缓存失败时回源数据库，进度事件通过 RabbitMQ 异步落库。暂停、拖动结束和停止会立即上报，播放期间每 10 秒发送一次心跳。',
  },
  {
    username: 'demo',
    clientRequestId: '9a908571-e394-4e85-8a50-617e8640dd02',
    title: '拼团名额控制：Redis 与数据库各自负责什么',
    content: 'Redis INCR 与唯一占位用于快速挡住并发流量，数据库唯一约束和条件更新负责最终正确性。\n\n占位后创建订单失败必须回补；支付和成团状态在本地事务中更新，成团通知通过 Outbox、重试和定时补偿收敛。',
  },
  {
    username: 'local-test-peer',
    clientRequestId: '9a908571-e394-4e85-8a50-617e8640dd03',
    title: '积分赛季榜：Bitmap、ZSet 和流水表的分工',
    content: 'Bitmap 适合记录每日签到事实，ZSet 适合实时排名，但两者都不应替代积分流水。\n\n消息消费者按至少一次投递设计，依靠事件幂等键避免重复入账；赛季结束后将榜单快照持久化，历史查询以数据库为准。',
  },
];

async function api(path, token, body, method = body === undefined ? 'GET' : 'POST') {
  const response = await fetch(`${base}${path}`, {
    method,
    signal: AbortSignal.timeout(15000),
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    ...(body === undefined ? {} : { body: JSON.stringify(body) }),
  });
  const result = await response.json().catch(() => null);
  if (!response.ok) throw new Error(`${method} ${path}: HTTP ${response.status} ${result?.message ?? '响应格式异常'}`);
  return result.data;
}

async function login(username) {
  return (await api('/api/iam/auth/login', undefined, { username, password })).accessToken;
}

const tokens = new Map();
for (const fixture of fixtures) {
  let token = tokens.get(fixture.username);
  if (!token) {
    token = await login(fixture.username);
    tokens.set(fixture.username, token);
  }
  let note = await api('/api/learning/notes', token, {
    clientRequestId: fixture.clientRequestId,
    courseId: null,
    chapterId: null,
    title: fixture.title,
    content: fixture.content,
    videoPositionMs: null,
  });
  if (note.status !== 'PUBLIC') {
    note = await api(`/api/learning/notes/${encodeURIComponent(note.id)}/status`, token,
      { status: 'PUBLIC', version: note.version }, 'PATCH');
  }
  console.log(`公开笔记就绪: ${note.title}`);
}
