import { Alert, Button, Card, Descriptions, Space } from 'antd';
import { usePoints } from './PointsContext';

export function SignInPanel() {
  const { signing, signError, signIn, sign, calendar } = usePoints();
  return <Card title="每日签到">
    <Space direction="vertical" size="middle" style={{ width: '100%' }}>
      <p>按 UTC 日期每日签到一次。积分通过消息队列异步发放，榜单可能稍后更新。</p>
      {signError ? <Alert type="error" showIcon message={signError} /> : null}
      {signIn ? <>
        <Alert type="success" showIcon message={`${signIn.signDate}：${signIn.duplicated ? '已签到，未重复奖励' : '签到成功'}`} />
        <Descriptions column={1} items={[
          { key: 'days', label: '该次连续签到', children: `${signIn.continuousDays} 天` },
          { key: 'reward', label: '该日奖励', children: `${signIn.rewardPoints} 积分（不代表已到账）` },
        ]} />
      </> : <p>{calendar && calendar.month === calendar.today.slice(0, 7)
        ? `${calendar.today}：${calendar.signedDates.includes(calendar.today) ? '今日已签到' : '今日尚未签到'}`
        : '今日状态尚未查询；点击签到后确认服务端结果。'}</p>}
      <Button type="primary" loading={signing} onClick={() => void sign()}>签到 / 确认今日签到</Button>
    </Space>
  </Card>;
}
