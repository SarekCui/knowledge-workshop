import { useEffect } from 'react';
import { Alert, Button, Empty, Skeleton, Typography } from 'antd';
import { Link, useParams } from 'react-router';
import { useLearning } from './LearningContext';
import { ChapterList } from './ChapterList';
import { Player } from './Player';
import { NoteProvider } from '../note/NoteContext';
import { NotePanel } from '../note/NotePanel';

export function CourseLearningPage() {
  const { courseId } = useParams();
  const { loadCourse, busy, error, chapters, playing } = useLearning();
  useEffect(() => loadCourse(courseId!), [courseId, loadCourse]);
  return <>
    <header className="page-heading course-page-heading">
    <Link to="/learning" className="back-link">← 返回我的学习</Link>
    <Typography.Title level={2}>课程学习</Typography.Title>
    <Link to={`/notes/course/${encodeURIComponent(courseId!)}`}>浏览这门课程的公开笔记 →</Link>
    <Typography.Paragraph type="secondary">专注眼前这一节，下一次从这里继续。</Typography.Paragraph>
    </header>
    {error ? <Alert type="error" showIcon message={error}
      action={<Button onClick={() => window.location.reload()}>重新加载</Button>} /> : null}
    <div className="learning-grid">
      <section className="player-panel" aria-label="学习空间">
      {playing ? <Player key={playing.session.sessionId} /> : busy ? <div className="player-placeholder" role="status"><Skeleton active />正在加载学习空间</div>
        : <div className="player-placeholder"><Empty description={error ? '学习空间暂时不可用，请重试' : '选择课程目录中的章节开始学习'} />
          <p className="muted">打开视频后，将自动恢复上次保存的位置。</p></div>}
      </section>
      <aside aria-label="章节导航"><ChapterList /></aside>
    </div>
    <NoteProvider key={courseId} courseId={courseId!}><NotePanel /></NoteProvider>
  </>;
}
