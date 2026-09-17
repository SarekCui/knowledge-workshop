import { Button, Card, Form, Input, Typography } from 'antd';
import { useAuth } from '../auth/AuthContext';

export function LoginPanel() {
  const { login, busy } = useAuth();
  return <Card className="login-card">
    <Typography.Title level={2}>欢迎回来</Typography.Title>
    <Typography.Paragraph type="secondary">登录知识工坊，继续你的学习旅程。</Typography.Paragraph>
    <Form layout="vertical" onFinish={login}>
      <Form.Item label="账号" name="username" rules={[{ required: true, message: '请输入账号' }]}>
        <Input size="large" autoComplete="username" placeholder="请输入账号" />
      </Form.Item>
      <Form.Item label="密码" name="password" rules={[{ required: true, message: '请输入密码' }]}>
        <Input.Password size="large" autoComplete="current-password" placeholder="请输入密码" />
      </Form.Item>
      <Button htmlType="submit" type="primary" size="large" block loading={busy}>登录</Button>
    </Form>
  </Card>;
}
