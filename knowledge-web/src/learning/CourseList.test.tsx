import { renderToStaticMarkup } from 'react-dom/server';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { CourseList } from './CourseList';
import type { Course } from '../api';

const state = vi.hoisted(() => ({
  courses: [] as Course[], busy: false, page: 1, total: 0,
  courseStatus: 'success', courseError: '', refreshCourses: () => {}, selectCourse: () => {},
}));
vi.mock('./LearningContext', () => ({ useLearning: () => state }));

describe('课程加载状态', () => {
  beforeEach(() => { state.courseStatus = 'success'; state.courseError = ''; state.courses = []; state.total = 0; });
  it('请求失败不显示无权益结论', () => {
    state.courseStatus = 'error'; state.courseError = 'learning 服务暂时不可用';
    const html = renderToStaticMarkup(<CourseList />);
    expect(html).toContain('课程加载失败');
    expect(html).toContain('learning 服务暂时不可用');
    expect(html).not.toContain('暂无有效课程权益');
  });
  it('只有成功查询空列表才显示无权益', () => {
    expect(renderToStaticMarkup(<CourseList />)).toContain('暂无有效课程权益');
  });
  it('加载中不显示无权益结论', () => {
    state.courseStatus = 'loading';
    const html = renderToStaticMarkup(<CourseList />);
    expect(html).toContain('正在加载课程');
    expect(html).not.toContain('暂无有效课程权益');
  });
  it('卡片只展示实际课程进度和续播位置', () => {
    state.courses = [{ courseId: 'course-1', title: '真实课程', completionRate: 25, totalVideos: 4,
      completedVideos: 1, videoId: 'video-1', coverUrl: null, resumePositionMs: 12345,
      lastLearnedAt: '2026-09-14T00:00:00Z' }];
    state.total = 1;
    const html = renderToStaticMarkup(<CourseList />);
    expect(html).toContain('真实课程');
    expect(html).toContain('上次播放至 0:12');
    expect(html).toContain('继续学习');
    expect(html).not.toContain('暂无有效课程权益');
    expect(html).not.toContain('ant-pagination');
  });
});
