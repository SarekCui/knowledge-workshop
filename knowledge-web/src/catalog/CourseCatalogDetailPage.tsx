import { Alert, Button, Card, Empty, Image, Skeleton, Tag, Typography } from 'antd';
import { Link, useLocation } from 'react-router';
import { formatDuration } from '../learning/formatDuration';
import { useCourseCatalog } from './CourseCatalogContext';
import { useAuth } from '../auth/AuthContext';

function formatPrice(priceCents: number) {
  return new Intl.NumberFormat('zh-CN', { style: 'currency', currency: 'CNY' }).format(priceCents / 100);
}

export function CourseCatalogDetailPage() {
  const { status, error, detail, store } = useCourseCatalog();
  const { status: authStatus } = useAuth();
  const location = useLocation();
  if (status === 'loading' && !detail) return <div role="status" className="catalog-loading"><Skeleton active /></div>;
  if (error) return <Alert type="error" showIcon message={error} action={<Button onClick={() => void store.load()}>重试</Button>} />;
  if (!detail) return <Empty description="课程不存在或尚未发布" />;
  const { course, chapters } = detail;
  return <>
    <Link to="/courses" className="back-link">← 返回课程广场</Link>
    <section className="catalog-detail-hero">
      <div className="catalog-detail-cover">
        {course.coverUrl ? <Image preview={false} src={course.coverUrl} alt={`${course.title}课程封面`} />
          : <div className="catalog-cover-placeholder" aria-hidden="true"><span>CODE</span><span>KNOWLEDGE WORKSHOP</span></div>}
      </div>
      <div className="catalog-detail-copy">
        <div><Tag color="blue">{course.categoryName}</Tag>{course.entitled ? <Tag color="green">已拥有权益</Tag> : null}</div>
        <Typography.Title>{course.title}</Typography.Title>
        <Typography.Paragraph>{course.summary}</Typography.Paragraph>
        <div className="catalog-detail-facts"><span>{course.chapterCount} 个章节</span><span>总时长 {formatDuration(course.totalDurationMs)}</span></div>
        <strong className="catalog-price">{formatPrice(course.priceCents)}</strong>
        {authStatus !== 'authenticated'
          ? <Link className="ant-btn ant-btn-primary ant-btn-lg" to="/login"
              state={{ from: location.pathname }}>登录后获取课程</Link>
          : course.entitled
          ? <Link className="ant-btn ant-btn-primary ant-btn-lg" to={`/learning/courses/${encodeURIComponent(course.id)}`}>继续学习</Link>
          : <Link className="ant-btn ant-btn-primary ant-btn-lg" to="/activities">查看拼团活动</Link>}
      </div>
    </section>
    <Card title="课程目录" className="catalog-outline">
      {chapters.length === 0 ? <Empty description="课程章节正在准备中" /> : <ol>
        {chapters.map((chapter, index) => <li key={chapter.id}>
          <span className="catalog-chapter-number">{String(index + 1).padStart(2, '0')}</span>
          <span>{chapter.title}</span>
          <time>{formatDuration(chapter.videoDurationMs)}</time>
        </li>)}
      </ol>}
    </Card>
  </>;
}
