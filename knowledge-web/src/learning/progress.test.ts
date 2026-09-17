import { describe, expect, it, vi } from 'vitest';
import { ProgressQueue, watchedRange } from './progress';
import { ApiError, type ProgressEvent } from '../api';

describe('播放进度', () => {
  it('连续播放计入，拖动和停顿不计入', () => {
    expect(watchedRange(1000, 2000, 1000, 1)).toEqual({ startMs: 1000, endMs: 2000 });
    expect(watchedRange(1000, 30000, 1000, 1)).toBeNull();
    expect(watchedRange(null, 30000, 1000, 1)).toBeNull();
    expect(watchedRange(1000, 2000, 10000, 1)).toBeNull();
  });
  const event: ProgressEvent = { eventId: 'same-event', sessionId: 'session', sessionEpoch: 1,
    sequence: 1, eventType: 'PAUSE', positionMs: 2000, playedRanges: [],
    clientOccurredAt: '2026-09-14T00:00:00Z', playbackRate: 1 };
  it('失败重试使用相同事件，而不是新幂等键', async () => {
    const sent: ProgressEvent[] = [];
    const queue = new ProgressQueue(async value => {
      sent.push(structuredClone(value));
      if (sent.length === 1) throw new Error('timeout');
    }, () => {});
    queue.push(event);
    await new Promise(resolve => setTimeout(resolve, 0));
    await queue.flush();
    expect(sent).toEqual([event, event]);
  });
  it('网络故障显示可恢复状态，原事件重试成功后清除警告', async () => {
    const notify = vi.fn();
    const send = vi.fn().mockRejectedValueOnce(new Error('offline')).mockResolvedValue(undefined);
    const queue = new ProgressQueue(send, notify);
    queue.push(event);
    await new Promise(resolve => setTimeout(resolve, 0));
    expect(notify).toHaveBeenLastCalledWith(expect.any(String), 'retrying');
    await queue.flush();
    expect(notify).toHaveBeenLastCalledWith('进度已提交');
    expect(send.mock.calls[0][0]).toEqual(send.mock.calls[1][0]);
  });
  it('会话冲突不再重试旧事件', async () => {
    let count = 0;
    const queue = new ProgressQueue(async () => { count++; throw new ApiError(409, '冲突'); }, () => {});
    queue.push(event);
    await new Promise(resolve => setTimeout(resolve, 0));
    await queue.flush();
    expect(count).toBe(1);
  });
  it('网络持续故障时队列有界，满时返回背压信号', () => {
    const queue = new ProgressQueue(() => new Promise<void>(() => {}), () => {});
    for (let index = 0; index < 100; index++) expect(queue.push(event)).toBe(true);
    expect(queue.push(event)).toBe(false);
    queue.stop();
    expect(queue.push(event)).toBe(false);
  });
});
