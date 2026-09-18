import { Tabs } from 'antd';
import { Link, useLocation, useNavigate } from 'react-router';
import { NoteProvider } from './NoteContext';
import { NotePanel } from './NotePanel';
import { NoteBrowseProvider } from './NoteBrowseContext';
import { NoteBrowsePage } from './NoteBrowsePage';

type ContentTab = 'mine' | 'liked' | 'favorites';

const tabRoutes: Record<ContentTab, string> = {
  mine: '/notes/mine',
  liked: '/notes/liked',
  favorites: '/notes/favorites',
};

export function MyNotesPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const activeTab: ContentTab = location.pathname.endsWith('/liked') ? 'liked'
    : location.pathname.endsWith('/favorites') ? 'favorites' : 'mine';
  return <section className="my-content-page"><header className="page-heading">
    <Link to="/notes">← 返回发现笔记</Link><h1>我的内容</h1>
    <p className="muted">集中管理你的创作、点赞与收藏。</p>
  </header>
    <Tabs className="my-content-tabs" activeKey={activeTab} onChange={key => navigate(tabRoutes[key as ContentTab])}
      items={[
        { key: 'mine', label: '我的笔记' },
        { key: 'liked', label: '点赞' },
        { key: 'favorites', label: '收藏' },
      ]} />
    {activeTab === 'mine'
      ? <NoteProvider courseId={null} paginated><NotePanel /></NoteProvider>
      : <NoteBrowseProvider key={activeTab} collection={activeTab}>
        <NoteBrowsePage embedded />
      </NoteBrowseProvider>}
  </section>;
}
