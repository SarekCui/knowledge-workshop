import { Button, Card, Image, Progress, Tag, Typography } from 'antd';
import { useLearning } from './LearningContext';
import { formatDuration } from './formatDuration';

export function CourseCard({ courseId }: { courseId: string }) {
  const { courses, busy, selectCourse } = useLearning();
  const course = courses.find(item => item.courseId === courseId);
  if (!course) return null;
  return <Card className="course-card">
    <div className="course-cover">
      {course.coverUrl ? <Image src={course.coverUrl} alt={course.title} preview={false}
        fallback="data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='480' height='270'%3E%3Crect width='480' height='270' fill='%23e7edf5'/%3E%3Ctext x='240' y='140' text-anchor='middle' fill='%23172b4d' font-size='24'%3EKnowledge Workshop%3C/text%3E%3C/svg%3E" />
        : <div className="course-cover-placeholder"><span aria-hidden="true">K / W</span><span>知识工坊 · 在线课程</span></div>}
    </div>
    <div className="course-card-content">
      <Tag color={course.completionRate === 100 ? 'green' : 'blue'}>{course.completionRate === 100 ? '已完成' : course.lastLearnedAt ? '学习中' : '待学习'}</Tag>
      <Typography.Title level={3}>{course.title}</Typography.Title>
      <Typography.Text type="secondary">已完成 {course.completedVideos} / {course.totalVideos} 个视频</Typography.Text>
      <Progress percent={course.completionRate} size={["100%", 6]} />
      <p className="course-resume">{course.lastLearnedAt ? `上次播放至 ${formatDuration(course.resumePositionMs)}` : '选择章节，开始第一段学习'}</p>
      <Button type="primary" block disabled={busy} onClick={() => void selectCourse(course.courseId)}>
        {course.lastLearnedAt ? '继续学习' : '查看章节'}
      </Button>
    </div>
  </Card>;
}
