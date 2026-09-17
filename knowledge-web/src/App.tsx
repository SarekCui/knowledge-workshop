import { Alert, Button, Layout, Result, Spin } from 'antd';
import { Link, Navigate, Outlet, Route, Routes, useLocation, useParams } from 'react-router';
import { useAuth } from './auth/AuthContext';
import { LearningProvider } from './learning/LearningContext';
import { LearningWorkspace } from './learning/LearningWorkspace';
import { CourseLearningPage } from './learning/CourseLearningPage';
import { LoginPanel } from './learning/LoginPanel';
import type { ReactNode } from 'react';
import { returnPath } from './auth/returnPath';
import { PurchaseProvider } from './purchase/PurchaseContext';
import type { PurchaseView } from './purchase/PurchaseStore';
import { ActivitiesPage } from './purchase/ActivitiesPage';
import { ActivityPage } from './purchase/ActivityPage';
import { OrdersPage } from './purchase/OrdersPage';
import { OrderPage } from './purchase/OrderPage';
import { PointsProvider } from './points/PointsContext';
import { PointsPage } from './points/PointsPage';
import { NoteBrowseProvider } from './note/NoteBrowseContext';
import { NoteBrowsePage } from './note/NoteBrowsePage';
import { MyNotesPage } from './note/MyNotesPage';
import type { NoteCollection } from './note/NoteBrowseStore';
import { AccountMenu } from './profile/AccountMenu';
import { AccountPage } from './profile/AccountPage';
import { CourseCatalogProvider } from './catalog/CourseCatalogContext';
import { CourseCatalogPage } from './catalog/CourseCatalogPage';
import { CourseCatalogDetailPage } from './catalog/CourseCatalogDetailPage';
import { MainNavigation } from './navigation/MainNavigation';

function NoteBrowseRoute({ collection = 'public' }: { collection?: NoteCollection }) {
  const location = useLocation();
  return <NoteBrowseProvider key={location.pathname} collection={collection}><NoteBrowsePage /></NoteBrowseProvider>;
}

function AuthBoundary() {
  const { status, restore, error } = useAuth();
  const location = useLocation();
  if (status === 'restoring') return <div role="status"><Spin /> 正在恢复登录</div>;
  if (status === 'error') return <Alert type="error" message="登录恢复失败" description={error}
    action={<Button onClick={() => void restore()}>重试</Button>} />;
  if (status !== 'authenticated') return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  return <Outlet />;
}
function LearningBoundary() {
  const location = useLocation();
  return <LearningProvider key={location.pathname}><Outlet /></LearningProvider>;
}
function CourseCatalogRoute() {
  const { courseId } = useParams();
  const location = useLocation();
  const view = courseId ? { kind: 'detail' as const, courseId } : { kind: 'list' as const };
  return <CourseCatalogProvider key={location.pathname} view={view}>
    {courseId ? <CourseCatalogDetailPage /> : <CourseCatalogPage />}
  </CourseCatalogProvider>;
}
function PurchaseRoute({ kind, children }: { kind: PurchaseView['kind']; children: ReactNode }) {
  const { activityId, orderId } = useParams();
  const location = useLocation();
  const view: PurchaseView = kind === 'activity' ? { kind, id: activityId! }
    : kind === 'order' ? { kind, id: orderId! } : { kind };
  return <PurchaseProvider key={location.pathname} view={view}>{children}</PurchaseProvider>;
}
function LoginPage() {
  const { status, error, restore, logout, busy } = useAuth();
  const location = useLocation();
  const candidate: unknown = location.state?.from;
  const target = returnPath(candidate);
  if (status === 'authenticated') return <Navigate to={target} replace />;
  if (status === 'restoring') return <div role="status"><Spin /> 正在恢复登录</div>;
  return <section className="login-layout">
    <div className="login-intro">
      <span className="eyebrow">KNOWLEDGE WORKSHOP</span>
      <h1>让知识，<br />成为你的下一步。</h1>
      <p>从一节课程开始，积累属于自己的知识。<br />每次回来，都可以从上次离开的地方继续。</p>
      <div className="login-feature"><span aria-hidden="true">01</span> 课程学习 · 随时继续</div>
      <div className="login-feature"><span aria-hidden="true">02</span> 播放进度 · 自动记录</div>
    </div>
    <div className="login-form-area">
    {error ? <Alert type="error" message={error} showIcon /> : null}
    {status === 'error' ? <Button onClick={() => void restore()}>重试恢复登录</Button> : null}
    {error.startsWith('退出请求未确认') ? <Button disabled={busy} onClick={() => void logout()}>重试退出</Button> : null}
    <LoginPanel />
    </div>
  </section>;
}
export function App() {
  return <Layout className="app-shell">
    <a className="skip-link" href="#main-content">跳转到主要内容</a>
    <Layout.Header className="app-header">
      <div className="header-inner">
      <Link to="/learning" className="brand"><span className="brand-mark" aria-hidden="true">K</span>知识工坊</Link>
      <MainNavigation />
      <AccountMenu />
      </div>
    </Layout.Header>
    <Layout.Content className="workspace" id="main-content" role="main"><Routes>
      <Route path="/" element={<Navigate to="/notes" replace />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/notes" element={<NoteBrowseRoute />} />
      <Route path="/notes/course/:courseId" element={<NoteBrowseRoute />} />
      <Route path="/notes/:noteId" element={<NoteBrowseRoute />} />
      <Route path="/courses" element={<CourseCatalogRoute />} />
      <Route path="/courses/:courseId" element={<CourseCatalogRoute />} />
      <Route element={<AuthBoundary />}>
        <Route path="/notes/mine" element={<MyNotesPage />} />
        <Route path="/notes/liked" element={<NoteBrowseRoute collection="liked" />} />
        <Route path="/notes/favorites" element={<NoteBrowseRoute collection="favorites" />} />
        <Route path="/account" element={<AccountPage />} />
        <Route path="/points" element={<PointsProvider><PointsPage /></PointsProvider>} />
        <Route element={<LearningBoundary />}>
        <Route path="/learning" element={<LearningWorkspace />} />
        <Route path="/learning/courses/:courseId" element={<CourseLearningPage />} />
        </Route>
        <Route path="/activities" element={<PurchaseRoute kind="activities"><ActivitiesPage /></PurchaseRoute>} />
        <Route path="/activities/:activityId" element={<PurchaseRoute kind="activity"><ActivityPage /></PurchaseRoute>} />
        <Route path="/orders" element={<PurchaseRoute kind="orders"><OrdersPage /></PurchaseRoute>} />
        <Route path="/orders/:orderId" element={<PurchaseRoute kind="order"><OrderPage /></PurchaseRoute>} />
      </Route>
      <Route path="*" element={<Result status="404" title="页面不存在" extra={<Link to="/learning">返回我的学习</Link>} />} />
    </Routes></Layout.Content>
  </Layout>;
}
