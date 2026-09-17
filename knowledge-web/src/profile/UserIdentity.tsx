import { Avatar, Space } from 'antd';
import { useUserDirectory } from './UserDirectoryContext';

export function UserIdentity({ userId, compact = false }: { userId?: string | null; compact?: boolean }) {
  const { profiles } = useUserDirectory();
  const fallback = userId ?? '未知用户';
  const profile = userId ? profiles[userId] : undefined;
  const name = profile?.nickname ?? fallback;
  return <Space size={compact ? 6 : 8} className="user-identity">
    <Avatar size={compact ? 'small' : 'default'} src={profile?.avatarUrl ?? undefined}>{name.slice(0, 1).toUpperCase()}</Avatar>
    <span>{name}</span>
  </Space>;
}
