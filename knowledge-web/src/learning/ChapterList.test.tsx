import { renderToStaticMarkup } from 'react-dom/server';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { Chapter } from '../api';
import { ChapterList } from './ChapterList';

const state = vi.hoisted(() => ({ chapters: [] as Chapter[], busy: false,
  playing: undefined as { chapter: Chapter } | undefined, openVideo: () => {} }));
vi.mock('./LearningContext', () => ({ useLearning: () => state }));

describe('课程目录', () => {
  beforeEach(() => { state.chapters = []; state.busy = false; state.playing = undefined; });
  it('当前章节和时长来自学习状态', () => {
    const chapter = { id: 'chapter-1', title: 'Oceans', videoId: 'video-1',
      videoUrl: '/oceans.mp4', videoDurationMs: 46613, videoVersion: 1 };
    state.chapters = [chapter]; state.playing = { chapter };
    const html = renderToStaticMarkup(<ChapterList />);
    expect(html).toContain('aria-current="true"');
    expect(html).toContain('0:46');
    expect(html).toContain('当前章节');
  });
  it('加载中不显示空目录', () => {
    state.busy = true;
    expect(renderToStaticMarkup(<ChapterList />)).not.toContain('暂无可学习章节');
  });
});
