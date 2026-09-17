import { createRoot } from 'react-dom/client';
import { ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import { BrowserRouter } from 'react-router';
import { AuthProvider } from './auth/AuthContext';
import { App } from './App';
import './style.css';
import { ProfileProvider } from './profile/ProfileContext';
import { UserDirectoryProvider } from './profile/UserDirectoryContext';
import { AvatarCropProvider } from './profile/AvatarCropContext';
import { AvatarCropDialog } from './profile/AvatarCropDialog';

createRoot(document.getElementById('root')!).render(
  <ConfigProvider locale={zhCN} theme={{
    token: {
      colorPrimary: '#245bd6',
      colorText: '#172b4d',
      colorTextSecondary: '#64748b',
      colorBgLayout: '#f5f7fb',
      colorBorderSecondary: '#e7edf5',
      borderRadius: 12,
      fontFamily: 'Inter, -apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif',
    },
  }}>
    <BrowserRouter>
      <AuthProvider>
        <ProfileProvider>
          <AvatarCropProvider>
            <UserDirectoryProvider>
              <App />
              <AvatarCropDialog />
            </UserDirectoryProvider>
          </AvatarCropProvider>
        </ProfileProvider>
      </AuthProvider>
    </BrowserRouter>
  </ConfigProvider>
);
