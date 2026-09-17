import { Space } from 'antd';
import { SignInPanel } from './SignInPanel';
import { SeasonLeaderboard } from './SeasonLeaderboard';
import { SignInCalendar } from './SignInCalendar';

export function PointsPage() {
  return <section><h1>签到与积分</h1>
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <SignInCalendar /><SignInPanel /><SeasonLeaderboard />
    </Space>
  </section>;
}
