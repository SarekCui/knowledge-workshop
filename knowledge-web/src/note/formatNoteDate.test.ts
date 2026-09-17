import { describe, expect, it } from 'vitest';
import { formatNoteDate } from './formatNoteDate';

describe('笔记时间展示', () => {
  it('未发布和非法时间不会显示 Invalid Date', () => {
    expect(formatNoteDate(null)).toBe('尚未发布');
    expect(formatNoteDate('not-a-date')).toBe('时间未知');
  });

  it('已发布时间使用中文可读格式', () => {
    expect(formatNoteDate('2026-09-15T02:00:00Z')).toContain('2026');
  });
});
