import { afterEach, describe, expect, it, vi } from 'vitest';
import type { KeyboardEvent } from 'react';
vi.mock('react', () => ({ useState: (initial: unknown) => [initial, vi.fn()] }));
import { seekVideo, usePlayerControls } from './usePlayerControls';

function createVideo() {
  return { currentTime: 15, duration: 46, readyState: 3, playbackRate: 1, volume: .95, muted: true,
    paused: true, play: vi.fn(async () => {}), pause: vi.fn(), requestFullscreen: vi.fn(async () => {}) };
}

describe('学习播放器控制', () => {
  afterEach(() => vi.unstubAllGlobals());
  it('快进后退限制在媒体边界，未就绪时不改位置', () => {
    const video = createVideo() as unknown as HTMLVideoElement;
    seekVideo(video, -30); expect(video.currentTime).toBe(0);
    seekVideo(video, 100); expect(video.currentTime).toBe(46);
    Object.defineProperty(video, 'readyState', { value: 0 });
    seekVideo(video, -10); expect(video.currentTime).toBe(46);
  });
  it('速度选项修改真实媒体播放速率', () => {
    const video = createVideo();
    const controls = usePlayerControls({ current: video as unknown as HTMLVideoElement });
    controls.changeRate(1.5);
    expect(video.playbackRate).toBe(1.5);
  });
  it('快捷键控制播放、跳转、音量和全屏，不劫持子控件或组合键', () => {
    vi.stubGlobal('document', { fullscreenElement: null });
    const video = createVideo();
    const controls = usePlayerControls({ current: video as unknown as HTMLVideoElement });
    const frame = {};
    const key = (value: string, target = frame, ctrlKey = false) => {
      const event = { key: value, target, currentTarget: frame, ctrlKey, preventDefault: vi.fn() };
      controls.onKeyDown(event as unknown as KeyboardEvent<HTMLElement>);
      return event;
    };
    key('ArrowRight'); expect(video.currentTime).toBe(25);
    key('ArrowLeft'); expect(video.currentTime).toBe(15);
    key('ArrowUp'); expect(video.volume).toBe(1); expect(video.muted).toBe(false);
    video.volume = .05; key('ArrowDown'); expect(video.volume).toBe(0);
    key(' '); expect(video.play).toHaveBeenCalledOnce();
    video.paused = false; key(' '); expect(video.pause).toHaveBeenCalledOnce();
    key('f'); expect(video.requestFullscreen).toHaveBeenCalledOnce();
    expect(key('ArrowRight', {}).preventDefault).not.toHaveBeenCalled();
    expect(key('ArrowRight', frame, true).preventDefault).not.toHaveBeenCalled();
    expect(video.currentTime).toBe(15);
  });
});
