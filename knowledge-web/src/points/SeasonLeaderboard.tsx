import { Alert, Button, Card, Table } from 'antd';
import { usePoints } from './PointsContext';

export function SeasonLeaderboard() {
  const { season, ranking, loading, rankingError, refresh } = usePoints();
  return <Card title={`${season} · 实时积分榜`} extra={<Button loading={loading} onClick={() => void refresh()}>刷新榜单</Button>}>
    <p>UTC 自然季度，展示前 100 名。榜单未收录不代表本人积分为零；积分到账后请刷新。</p>
    {rankingError ? <Alert type="error" showIcon message={rankingError} /> :
      <Table rowKey="userId" loading={loading} dataSource={ranking} pagination={{ pageSize: 20, showSizeChanger: false }}
        locale={{ emptyText: loading ? '正在读取榜单' : '当前榜单暂无记录' }} columns={[
          { title: '排名', dataIndex: 'rank', width: 80 },
          { title: '用户', dataIndex: 'userId', ellipsis: true },
          { title: '赛季积分', dataIndex: 'score', width: 120 },
        ]} />}
  </Card>;
}
