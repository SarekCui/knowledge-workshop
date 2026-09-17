import { describe, expect, it } from 'vitest';
import { formatDuration } from './formatDuration';

describe('播放时间展示', () => {
  it('显示到秒，不改变实际毫秒进度', () => {
    expect(formatDuration(12345)).toBe('0:12');
    expect(formatDuration(46613)).toBe('0:46');
    expect(formatDuration(3600123)).toBe('60:00');
  });
  it('零位置与负位置显示为零', () => {
    expect(formatDuration(0)).toBe('0:00');
    expect(formatDuration(-100)).toBe('0:00');
  });
});
