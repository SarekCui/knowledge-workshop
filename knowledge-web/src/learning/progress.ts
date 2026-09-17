import { ApiError, type ProgressEvent, type Range } from '../api';

// Only short, continuous advances count as watched; seeking resets the anchor.
export function watchedRange(previousMs: number | null, currentMs: number,
  elapsedMs: number, rate: number): Range | null {
  if (previousMs === null || currentMs <= previousMs || elapsedMs <= 0 || elapsedMs > 3000) return null;
  if (currentMs - previousMs > elapsedMs * rate + 500) return null;
  return { startMs: previousMs, endMs: currentMs };
}

export class ProgressQueue {
  private running = false;
  private stopped = false;
  private events: ProgressEvent[] = [];
  constructor(private send: (event: ProgressEvent) => Promise<void>,
    private notify: (message: string, feedback?: 'normal' | 'retrying' | 'blocked') => void) {}

  push(event: ProgressEvent) {
    if (this.stopped) return false;
    if (this.events.length >= 100) {
      this.notify('待同步事件已达上限，请暂停播放并等待网络恢复', 'retrying');
      return false;
    }
    this.events.push(structuredClone(event));
    void this.flush();
    return true;
  }
  stop() { this.stopped = true; }
  async flush() {
    if (this.running || this.stopped) return;
    this.running = true;
    try {
      while (this.events.length && !this.stopped) {
        try {
          await this.send(this.events[0]);
          this.events.shift();
          this.notify(this.events.length ? '正在同步进度' : '进度已提交');
        } catch (error) {
          if (error instanceof ApiError && error.status < 500 && error.status !== 429) {
            this.stopped = true;
            this.notify(error.status === 409 ? '会话已失效，请重新打开视频' : error.message, 'blocked');
          } else this.notify('进度同步失败，将自动重试，请暂时保留此页面', 'retrying');
          break;
        }
      }
    } finally { this.running = false; }
  }
}
