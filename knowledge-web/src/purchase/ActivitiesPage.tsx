import { Card, Empty, Typography } from 'antd';
import { Link } from 'react-router';
import { usePurchase } from './PurchaseContext';
import { PurchaseFeedback } from './PurchaseFeedback';
import { PurchasePagination } from './PurchasePagination';
import { money } from './types';

export function ActivitiesPage() {
  const { activities, status } = usePurchase();
  return <><header className="page-heading"><Typography.Title level={2}>拼团活动</Typography.Title>
    <Typography.Paragraph type="secondary">选择活动与可加入的团，成团后获得课程权益。</Typography.Paragraph></header>
    <PurchaseFeedback />
    {status === 'success' ? activities.length ? <div className="course-grid">{activities.map(activity => <Card key={activity.id}>
      <Typography.Title level={3}>活动 {activity.id}</Typography.Title>
      <p className="resource-id">课程：{activity.courseId}</p><p>{activity.targetCount} 人团 · {money(activity.priceCents)}</p>
      <Link to={`/activities/${encodeURIComponent(activity.id)}`}>查看课程与可用团 →</Link>
    </Card>)}</div> : <Empty description="暂无可参与的拼团活动" /> : null}
    <PurchasePagination />
  </>;
}
