import { describe, expect, it } from 'vitest';
import { noteImageId } from './AuthenticatedNoteImage';

describe('技术笔记图片地址', () => {
  it('只接受站内稳定图片标识', () => {
    expect(noteImageId('/api/learning/note-images/11111111-1111-1111-1111-111111111111'))
      .toBe('11111111-1111-1111-1111-111111111111');
    expect(noteImageId('https://tracker.example/pixel.png')).toBeUndefined();
    expect(noteImageId('/api/learning/note-images/not-a-uuid')).toBeUndefined();
  });
});
