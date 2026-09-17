import { Alert, Button, Card, Empty, Image, Input, Pagination, Skeleton, Tag, Typography } from 'antd';
import type { FormEvent } from 'react';
import { Link } from 'react-router';
import { formatDuration } from '../learning/formatDuration';
import { useCourseCatalog } from './CourseCatalogContext';

function formatPrice(priceCents: number) {
  return new Intl.NumberFormat('zh-CN', { style: 'currency', currency: 'CNY' }).format(priceCents / 100);
}

function CourseCard({ courseId }: { courseId: string }) {
  const { courses } = useCourseCatalog();
  const course = courses.find(item => item.id === courseId);
  if (!course) return null;
  return <Card className="catalog-card">
    <Link to={`/courses/${encodeURIComponent(course.id)}`} aria-label={`查看课程：${course.title}`}>
      <div className="catalog-cover">
        {course.coverUrl ? <Image preview={false} src={course.coverUrl} alt="" />
          : <div className="catalog-cover-placeholder" aria-hidden="true"><span>CODE</span><span>KNOWLEDGE WORKSHOP</span></div>}
      </div>
    </Link>
    <div className="catalog-card-body">
      <Typography.Title level={3}><Link to={`/courses/${encodeURIComponent(course.id)}`}>{course.title}</Link></Typography.Title>
      <div className="catalog-tags" aria-label="课程标签">
        <Tag color="blue">{course.categoryName}</Tag>
        {course.entitled ? <Tag color="green">已加入学习</Tag> : null}
      </div>
      <Typography.Paragraph className="catalog-summary" ellipsis={{ rows: 2 }}>{course.summary}</Typography.Paragraph>
      <div className="catalog-meta"><span>{course.chapterCount} 节</span><span>{formatDuration(course.totalDurationMs)}</span></div>
      <div className="catalog-card-footer"><strong>{formatPrice(course.priceCents)}</strong>
        <Link to={`/courses/${encodeURIComponent(course.id)}`}>查看详情</Link></div>
    </div>
  </Card>;
}

export function CourseCatalogPage() {
  const { status, error, categories, courses, keyword, categoryId, page, total, store } = useCourseCatalog();
  const submit = (event: FormEvent) => { event.preventDefault(); void store.search(); };
  return <>
    <header className="page-heading catalog-heading">
      <span className="eyebrow">COURSE CATALOG</span>
      <Typography.Title level={2}>课程广场</Typography.Title>
      <Typography.Paragraph type="secondary">发现适合你的技术课程，购买权益后即可进入完整学习空间。</Typography.Paragraph>
    </header>
    <section className="catalog-toolbar" aria-label="筛选课程">
      <div className="catalog-categories" role="group" aria-label="课程分类">
        <Button type={categoryId === '' ? 'primary' : 'default'} onClick={() => void store.selectCategory('')}>全部课程</Button>
        {categories.map(category => <Button key={category.id} type={categoryId === category.id ? 'primary' : 'default'}
          onClick={() => void store.selectCategory(category.id)}>{category.name}</Button>)}
      </div>
      <form className="catalog-search" role="search" onSubmit={submit}>
        <label className="visually-hidden" htmlFor="course-keyword">搜索课程</label>
        <Input id="course-keyword" value={keyword} allowClear placeholder="搜索课程名称或简介"
          onChange={event => store.setKeyword(event.target.value)} />
        <Button htmlType="submit" type="primary">搜索</Button>
      </form>
    </section>
    {error ? <Alert type="error" showIcon message={error} action={<Button onClick={() => void store.load()}>重试</Button>} /> : null}
    {status === 'loading' && courses.length === 0 ? <div className="catalog-loading" role="status"><Skeleton active /></div> : null}
    {status !== 'loading' && courses.length === 0 && !error ? <Empty description="没有找到符合条件的课程" /> : null}
    {courses.length > 0 ? <section className="catalog-grid" aria-label="课程列表">
      {courses.map(course => <CourseCard key={course.id} courseId={course.id} />)}
    </section> : null}
    {total > 12 ? <Pagination className="course-pagination" current={page} pageSize={12} total={total}
      showSizeChanger={false} onChange={value => void store.changePage(value)} /> : null}
  </>;
}
