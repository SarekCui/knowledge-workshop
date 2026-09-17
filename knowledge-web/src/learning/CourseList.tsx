import { Alert, Button, Empty, Pagination, Skeleton, Typography } from 'antd';
import { useLearning } from './LearningContext';
import { CourseCard } from './CourseCard';

export function CourseList() {
  const { courses, busy, page, total, courseStatus, courseError, refreshCourses } = useLearning();
  return <section aria-label="可学习课程">
    <div className="section-heading"><h2>可学习课程{courseStatus === 'success' ? <span className="count-badge">{total}</span> : null}</h2>
      <Button disabled={busy} onClick={() => void refreshCourses(page)}>刷新</Button></div>
    {courseStatus === 'error' ? <Alert type="error" showIcon message="课程加载失败"
      description={courseError} action={<Button disabled={busy} onClick={() => void refreshCourses()}>重试</Button>} />
      : courseStatus === 'loading' ? <div role="status" aria-live="polite" className="loading-card">正在加载课程<Skeleton active /></div>
      : courseStatus === 'idle' ? <Typography.Text type="secondary">请刷新加载课程</Typography.Text>
      : <>
        {courses.length ? <div className="course-grid">{courses.map(course => <CourseCard key={course.courseId} courseId={course.courseId} />)}</div>
          : <div className="empty-card"><Empty description="暂无有效课程权益" /></div>}
        {total > 20 ? <Pagination className="course-pagination" current={page} total={total} pageSize={20} showSizeChanger={false}
          disabled={busy} onChange={value => void refreshCourses(value)} /> : null}
      </>}
  </section>;
}
