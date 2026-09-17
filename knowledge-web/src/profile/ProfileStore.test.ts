import { describe, expect, it, vi } from 'vitest';
import { ProfileStore } from './ProfileStore';
import type { UserProfile } from './types';

const profile: UserProfile = {
  userId: 'user-1', username: 'demo', nickname: '学习者', avatarUrl: null, bio: null, version: 2,
};

describe('个人资料状态', () => {
  it('修改资料携带当前版本并接受服务端新版本', async () => {
    const transport = vi.fn().mockResolvedValueOnce(profile).mockResolvedValueOnce({ ...profile, nickname: '新昵称', version: 3 });
    const store = new ProfileStore(transport);
    await store.load();
    await store.update('新昵称', '介绍');
    expect(transport).toHaveBeenLastCalledWith('/api/iam/profile',
      { nickname: '新昵称', bio: '介绍', version: 2 }, { method: 'PUT' });
    expect(store.getSnapshot().profile?.version).toBe(3);
  });

  it('头像使用multipart并复用当前资料版本', async () => {
    const transport = vi.fn().mockResolvedValueOnce(profile).mockResolvedValueOnce({ ...profile, avatarUrl: 'signed', version: 3 });
    const store = new ProfileStore(transport);
    await store.load();
    const file = new File([new Uint8Array([1, 2])], 'avatar.png', { type: 'image/png' });
    await store.uploadAvatar(file);
    const body = transport.mock.calls[1][1] as FormData;
    expect(body.get('version')).toBe('2');
    expect(body.get('file')).toBe(file);
    expect(transport.mock.calls[1][2]).toEqual({ method: 'PUT' });
  });
});
