import { Avatar, Button, Dropdown, Space, type MenuProps } from 'antd';
import { Link, useLocation } from 'react-router';
import { useAuth } from '../auth/AuthContext';
import { useProfile } from './ProfileContext';

export function AccountMenu() {
  const { status: authStatus, logout, busy: authBusy } = useAuth();
  const { profile, status } = useProfile();
  const location = useLocation();
  if (authStatus === 'restoring') return <Button className="account-trigger" loading aria-label="正在恢复登录" />;
  if (authStatus !== 'authenticated') return <Link className="ant-btn account-login" to="/login"
    state={{ from: location.pathname }}>登录</Link>;
  const name = profile?.nickname ?? profile?.username ?? '我的账号';
  const items: MenuProps['items'] = [
    { key: 'learning', label: <Link to="/learning">我的课程</Link> },
    { key: 'notes', label: <Link to="/notes/mine">我的内容</Link> },
    { type: 'divider' },
    { key: 'orders', label: <Link to="/orders">我的订单</Link> },
    { key: 'points', label: <Link to="/points">签到积分</Link> },
    { key: 'profile', label: <Link to="/account">个人资料</Link> },
    { type: 'divider' },
    { key: 'logout', danger: true, disabled: authBusy, label: '退出登录', onClick: () => void logout() },
  ];
  return <Dropdown menu={{ items }} placement="bottomRight" trigger={['click']}>
    <Button className="account-trigger" loading={status === 'loading'}>
      <Space><Avatar size="small" src={profile?.avatarUrl ?? undefined}>{name.slice(0, 1).toUpperCase()}</Avatar>
        <span className="account-name">{name}</span></Space>
    </Button>
  </Dropdown>;
}
