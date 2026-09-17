import type { NoteTransport } from '../note/NoteStore';
import type { UserProfile } from './types';

interface Snapshot {
  status: 'idle' | 'loading' | 'success' | 'error';
  profile?: UserProfile;
  busy: boolean;
  error: string;
  notice: string;
}

export class ProfileStore {
  private state: Snapshot = { status: 'idle', busy: false, error: '', notice: '' };
  private listeners = new Set<() => void>();
  private generation = 0;

  constructor(private transport: NoteTransport) {}
  getSnapshot = () => this.state;
  subscribe = (listener: () => void) => { this.listeners.add(listener); return () => { this.listeners.delete(listener); }; };
  private publish(change: Partial<Snapshot>) {
    this.state = { ...this.state, ...change };
    this.listeners.forEach(listener => listener());
  }
  reset = () => { ++this.generation; this.publish({ status: 'idle', profile: undefined, busy: false, error: '', notice: '' }); };
  load = async () => {
    const generation = ++this.generation;
    this.publish({ status: 'loading', error: '' });
    try {
      const profile = await this.transport<UserProfile>('/api/iam/profile');
      if (generation === this.generation) this.publish({ status: 'success', profile });
    } catch (error) {
      if (generation === this.generation) this.publish({ status: 'error', error: this.message(error) });
    }
  };
  update = (nickname: string, bio: string) => this.perform(async profile => {
    const updated = await this.transport<UserProfile>('/api/iam/profile', {
      nickname, bio, version: profile.version,
    }, { method: 'PUT' });
    this.publish({ profile: updated, notice: '个人资料已保存' });
  });
  uploadAvatar = (file: File) => this.perform(async profile => {
    if (!['image/jpeg', 'image/png'].includes(file.type)) throw new Error('请选择 JPEG 或 PNG 图片');
    if (file.size > 2 * 1024 * 1024) throw new Error('头像文件不能超过 2MB');
    const body = new FormData();
    body.append('file', file);
    body.append('version', String(profile.version));
    const updated = await this.transport<UserProfile>('/api/iam/profile/avatar', body, { method: 'PUT' });
    this.publish({ profile: updated, notice: '头像已更新' });
  });
  private async perform(operation: (profile: UserProfile) => Promise<void>) {
    if (this.state.busy || !this.state.profile) return;
    this.publish({ busy: true, error: '', notice: '' });
    try { await operation(this.state.profile); }
    catch (error) { this.publish({ error: this.message(error) }); }
    finally { this.publish({ busy: false }); }
  }
  private message(error: unknown) { return error instanceof Error ? error.message : '请求失败，请重试'; }
}
