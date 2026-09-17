import { useEffect } from 'react';
import { Typography } from 'antd';
import { CourseList } from './CourseList';
import { useLearning } from './LearningContext';

export function LearningWorkspace() {
  const { refreshCourses } = useLearning();
  useEffect(() => { void refreshCourses(); }, [refreshCourses]);
  return <>
    <header className="page-heading">
    <span className="eyebrow">MY LEARNING</span>
    <Typography.Title level={2}>我的学习</Typography.Title>
    <Typography.Paragraph type="secondary">从上次离开的地方继续，让每一段学习都有记录。</Typography.Paragraph>
    </header>
    <CourseList />
  </>;
}
