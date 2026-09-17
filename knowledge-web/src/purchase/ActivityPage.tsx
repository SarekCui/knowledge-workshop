import { Alert, Button, Card, Empty, List, Popconfirm, Typography } from 'antd';
import { Link } from 'react-router';
import { usePurchase } from './PurchaseContext';
import { PurchaseFeedback } from './PurchaseFeedback';
import { PurchasePagination } from './PurchasePagination';
import { money } from './types';

export function ActivityPage() {
  const { activity, groups, course, courseError, status, busy, join } = usePurchase();
  return <><Link to="/activities">← 返回拼团活动</Link><PurchaseFeedback />
    {status === 'success' && activity ? <>
      <header className="page-heading"><Typography.Title level={2}>{course?.title ?? '拼团活动详情'}</Typography.Title>
      {courseError ? <Alert type="warning" message={`课程介绍加载失败：${courseError}`} /> : <p>{course?.summary}</p>}
      <p>{money(activity.priceCents)} · {activity.targetCount} 人成团 · 每人最多参与 {activity.maxJoinPerUser} 次</p></header>
      <Card title="可加入的团"><List dataSource={groups} locale={{ emptyText: <Empty description="暂无可加入的团，请等待运营开团" /> }}
        renderItem={group => <List.Item className="purchase-row"><div><strong className="resource-id">团 {group.id}</strong>
          <p>已确认 {group.confirmedCount} / {group.targetCount} 人</p><p className="muted">截止 {new Date(group.expiresAt).toLocaleString('zh-CN')}</p></div>
          <Popconfirm title="确认参加这个团？" description="将创建待支付订单；同一用户重复参团返回已有订单。" onConfirm={() => join(group.id)}>
            <Button type="primary" disabled={busy || group.status !== 'FORMING' || Date.parse(group.expiresAt) <= Date.now()}>参团</Button>
          </Popconfirm></List.Item>} /><PurchasePagination /></Card>
    </> : null}
  </>;
}
