import type { NoteTransport } from '../note/NoteStore';
import type { PublicUserProfile } from './types';

interface Snapshot { profiles: Record<string, PublicUserProfile>; }

export class UserDirectoryStore {
  private state: Snapshot = { profiles: {} };
  private listeners = new Set<() => void>();
  private pending = new Set<string>();

  constructor(private transport: NoteTransport) {}
  getSnapshot = () => this.state;
  subscribe = (listener: () => void) => { this.listeners.add(listener); return () => { this.listeners.delete(listener); }; };
  reset = () => { this.pending.clear(); this.state = { profiles: {} }; this.listeners.forEach(listener => listener()); };
  upsert = (profile: PublicUserProfile) => {
    this.state = { profiles: { ...this.state.profiles, [profile.userId]: profile } };
    this.listeners.forEach(listener => listener());
  };
  ensure = async (userIds: Array<string | null | undefined>) => {
    const ids = [...new Set(userIds.filter((id): id is string => Boolean(id)))]
      .filter(id => !this.state.profiles[id] && !this.pending.has(id)).slice(0, 100);
    if (!ids.length) return;
    ids.forEach(id => this.pending.add(id));
    try {
      const query = new URLSearchParams();
      ids.forEach(id => query.append('userIds', id));
      const profiles = await this.transport<PublicUserProfile[]>(`/api/iam/users/public?${query}`);
      const additions = Object.fromEntries(profiles.map(profile => [profile.userId, profile]));
      this.state = { profiles: { ...this.state.profiles, ...additions } };
      this.listeners.forEach(listener => listener());
    } catch {
      // 用户公开资料只是展示增强，失败时保留可识别的用户 ID，不遮蔽笔记与评论主内容。
    } finally {
      ids.forEach(id => this.pending.delete(id));
    }
  };
}
