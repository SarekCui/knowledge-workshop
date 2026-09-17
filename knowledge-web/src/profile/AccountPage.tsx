import { Alert, Avatar, Button, Card, Form, Input, Skeleton, Space, Typography, Upload } from 'antd';
import { useEffect } from 'react';
import { useProfile } from './ProfileContext';
import { useAvatarCrop } from './AvatarCropContext';

interface ProfileForm { nickname: string; bio: string; }

export function AccountPage() {
  const { status, profile, busy, error, notice, store } = useProfile();
  const [form] = Form.useForm<ProfileForm>();
  const crop = useAvatarCrop();
  useEffect(() => {
    if (profile) form.setFieldsValue({ nickname: profile.nickname, bio: profile.bio ?? '' });
  }, [profile, form]);
  if ((status === 'idle' || status === 'loading') && !profile) return <Skeleton active />;
  if (!profile) return <Alert type="error" showIcon message={error || '个人资料暂时不可用'}
    action={<Button onClick={() => void store.load()}>重试</Button>} />;
  return <section className="account-page">
    <header className="page-heading"><span className="eyebrow">ACCOUNT</span><h1>个人账号</h1>
      <p className="muted">管理你的公开昵称、头像和个人简介。</p></header>
    {error ? <Alert type="error" showIcon message={error} /> : null}
    {crop.error ? <Alert type="error" showIcon message={crop.error} /> : null}
    {notice ? <Alert type="success" showIcon message={notice} /> : null}
    <div className="account-grid">
      <Card className="avatar-card">
        <Avatar size={112} src={profile.avatarUrl ?? undefined}>{profile.nickname.slice(0, 1).toUpperCase()}</Avatar>
        <Typography.Title level={4}>{profile.nickname}</Typography.Title>
        <p className="muted">JPEG / PNG，最大 2MB。选择后可拖动和缩放，自主决定头像显示范围。</p>
        <Upload accept="image/jpeg,image/png" showUploadList={false} disabled={busy}
          beforeUpload={file => { crop.open(file); return Upload.LIST_IGNORE; }}>
          <Button loading={busy}>更换头像</Button>
        </Upload>
      </Card>
      <Card title="公开资料">
        <Form form={form} layout="vertical" requiredMark="optional"
          onFinish={values => void store.update(values.nickname, values.bio)}>
          <Form.Item label="登录账号"><Input value={profile.username} disabled /></Form.Item>
          <Form.Item name="nickname" label="公开昵称" rules={[{ required: true, message: '请输入昵称' }, { max: 32 }]}>
            <Input maxLength={32} showCount />
          </Form.Item>
          <Form.Item name="bio" label="个人简介" rules={[{ max: 200 }]}>
            <Input.TextArea maxLength={200} showCount autoSize={{ minRows: 4, maxRows: 7 }} />
          </Form.Item>
          <Space><Button type="primary" htmlType="submit" loading={busy}>保存资料</Button>
            <Button disabled={busy} onClick={() => form.setFieldsValue({ nickname: profile.nickname, bio: profile.bio ?? '' })}>恢复</Button></Space>
        </Form>
      </Card>
    </div>
  </section>;
}
