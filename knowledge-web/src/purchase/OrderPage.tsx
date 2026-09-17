import { Alert, Button, Card, Descriptions, Popconfirm, Space, Typography } from 'antd';
import { Link } from 'react-router';
import { usePurchase } from './PurchaseContext';
import { PurchaseFeedback } from './PurchaseFeedback';
import { money, orderLabel } from './types';

export function OrderPage() {
  const { order, groups, status, busy, store } = usePurchase();
  return <><Link to="/orders">← 返回我的订单</Link><Typography.Title level={2}>订单详情</Typography.Title>
    <PurchaseFeedback />{status === 'success' && order ? <Card>
      <Descriptions column={1} items={[
        { key: 'id', label: '订单', children: <span className="resource-id">{order.orderId}</span> },
        { key: 'course', label: '课程', children: <span className="resource-id">{order.courseId}</span> },
        { key: 'amount', label: '金额', children: money(order.amountCents) },
        { key: 'status', label: '状态', children: orderLabel(order.status) },
        { key: 'group', label: '拼团', children: !groups[0] ? '状态未知，请刷新查询' : groups[0].status === 'FORMED' ? '已成团' : `当前状态 ${groups[0].status}（已确认 ${groups[0].confirmedCount} 人）` },
      ]} />
      <Alert className="payment-hint" type="warning" showIcon message="仅模拟支付，不扣真实资金。支付成功不等于成团或权益已经发放。" />
      <Space wrap>
        {order.status === 'PENDING_PAYMENT' ? <Popconfirm title={`确认模拟支付 ${money(order.amountCents)}？`} description="只测试业务流程，不接入支付渠道。" onConfirm={() => store.pay()}>
          <Button type="primary" disabled={busy}>模拟支付</Button></Popconfirm> : null}
        <Button disabled={busy} onClick={() => void store.load()}>刷新订单 / 拼团</Button>
        <Link to="/learning">前往我的学习</Link>
      </Space>
    </Card> : null}
  </>;
}
