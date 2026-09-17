import { Card, Empty, List, Tag, Typography } from 'antd';
import { Link } from 'react-router';
import { usePurchase } from './PurchaseContext';
import { PurchaseFeedback } from './PurchaseFeedback';
import { PurchasePagination } from './PurchasePagination';
import { money, orderLabel } from './types';

export function OrdersPage() {
  const { orders, status } = usePurchase();
  return <><header className="page-heading"><Typography.Title level={2}>我的订单</Typography.Title>
    <Typography.Paragraph type="secondary">支付与成团状态以服务端查询结果为准。</Typography.Paragraph></header>
    <PurchaseFeedback />{status === 'success' ? <Card><List dataSource={orders} locale={{ emptyText: <Empty description="暂无订单" /> }}
      renderItem={order => <List.Item className="purchase-row"><div><strong className="resource-id">订单 {order.orderId}</strong>
        <p>{money(order.amountCents)} · <Tag>{orderLabel(order.status)}</Tag></p></div>
        <Link to={`/orders/${encodeURIComponent(order.orderId)}`}>查看详情</Link></List.Item>} /><PurchasePagination /></Card> : null}
  </>;
}
