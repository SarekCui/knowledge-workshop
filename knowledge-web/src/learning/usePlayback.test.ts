import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import type { ProgressEvent } from '../api';

const harness = vi.hoisted(() => ({
  effect: undefined as undefined | (() => () => void),
  video: undefined as undefined | EventTarget,
  resumePositionMs: 0,
  request: vi.fn(async (_path?: string, _body?: unknown) => ({ accepted: true, cacheUpdated: true })),
}));
vi.mock('react', () => ({
  useEffect: (effect: () => () => void) => { harness.effect = effect; },
  useRef: () => ({ current: harness.video }),
  useState: (initial: unknown) => [initial, vi.fn()],
}));
vi.mock('../api', async importOriginal => ({
  ...await importOriginal<typeof import('../api')>(), request: harness.request,
}));
vi.mock('./LearningContext', () => ({ useLearning: () => ({ apiRequest: harness.request, playing: {
  chapter: { videoId: 'video', videoDurationMs: 60000 },
  session: { sessionId: 'session', sessionEpoch: 1, resumePositionMs: harness.resumePositionMs },
} }) }));
import { usePlayback } from './usePlayback';

class TestVideo extends EventTarget {
  currentTime = 0;
  duration = 60;
  readyState = 3;
  paused = true;
  seeking = false;
  playbackRate = 1;
  pause() { this.paused = true; this.dispatchEvent(new Event('pause')); }
}
describe('续播上报策略', () => {
  let video: TestVideo;
  let cleanup: () => void;
  const events = () => harness.request.mock.calls.map(call => (call as unknown[])[1] as ProgressEvent);
  beforeEach(() => {
    vi.useFakeTimers();
    vi.stubGlobal('window', globalThis);
    vi.stubGlobal('document', new EventTarget());
    harness.request.mockClear();
    harness.resumePositionMs = 0;
    video = new TestVideo();
    harness.video = video;
    cleanup = () => {};
  });
  const mount = () => { usePlayback(); cleanup = harness.effect!(); };
  afterEach(() => { cleanup?.(); vi.useRealTimers(); vi.unstubAllGlobals(); });

  it('播放满10秒上报，位置未变时跳过下一次心跳', async () => {
    mount();
    video.paused = false;
    video.currentTime = 9;
    await vi.advanceTimersByTimeAsync(9999);
    expect(events()).toHaveLength(0);
    video.currentTime = 10;
    await vi.advanceTimersByTimeAsync(1);
    expect(events()).toMatchObject([{ eventType: 'HEARTBEAT', positionMs: 10000 }]);
    await vi.advanceTimersByTimeAsync(10000);
    expect(events()).toHaveLength(1);
  });
  it('暂停立即保存毫秒位置，不等待心跳', async () => {
    mount();
    video.currentTime = 12.345;
    video.pause();
    await vi.advanceTimersByTimeAsync(0);
    expect(events()).toMatchObject([{ eventType: 'PAUSE', positionMs: 12345 }]);
  });
  it('拖动过程中不提交中间位置，结束才提交目标位置', async () => {
    mount();
    video.seeking = true;
    video.currentTime = 20;
    video.dispatchEvent(new Event('seeking'));
    video.pause();
    await vi.advanceTimersByTimeAsync(0);
    expect(events()).toHaveLength(0);
    video.currentTime = 25.123;
    video.seeking = false;
    video.dispatchEvent(new Event('seeked'));
    await vi.advanceTimersByTimeAsync(0);
    expect(events()).toMatchObject([{ eventType: 'SEEKED', positionMs: 25123, playedRanges: [] }]);
  });
  it('恢复定位过程中不提交暂态位置，重复metadata不重新定位', async () => {
    harness.resumePositionMs = 27000;
    video.seeking = true;
    mount();
    video.currentTime = 22;
    video.dispatchEvent(new Event('seeking'));
    video.pause();
    await vi.advanceTimersByTimeAsync(0);
    expect(events()).toHaveLength(0);
    video.currentTime = 27;
    video.seeking = false;
    video.dispatchEvent(new Event('seeked'));
    await vi.advanceTimersByTimeAsync(0);
    expect(events()).toHaveLength(0);
    video.currentTime = 30;
    video.dispatchEvent(new Event('loadedmetadata'));
    expect(video.currentTime).toBe(30);
  });

  it('播放结束立即提交最终位置', async () => {
    mount();
    video.currentTime = 60;
    video.dispatchEvent(new Event('ended'));
    await vi.advanceTimersByTimeAsync(0);
    expect(events()).toMatchObject([{ eventType: 'ENDED', positionMs: 60000 }]);
  });

  it('页面隐藏及卸载尝试提交最终位置，卸载清除心跳', async () => {
    mount();
    video.currentTime = 12.345;
    Object.defineProperty(document, 'visibilityState', { value: 'hidden' });
    document.dispatchEvent(new Event('visibilitychange'));
    await vi.advanceTimersByTimeAsync(0);
    expect(events()).toMatchObject([{ eventType: 'EXIT', positionMs: 12345 }]);
    cleanup();
    cleanup = () => {};
    await vi.advanceTimersByTimeAsync(0);
    expect(events()).toHaveLength(2);
    video.paused = false;
    video.currentTime = 20;
    await vi.advanceTimersByTimeAsync(20000);
    expect(events()).toHaveLength(2);
  });
});
