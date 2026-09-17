import { renderToStaticMarkup } from 'react-dom/server';
import { describe, expect, it } from 'vitest';
import { NoteReactionButton, NoteReactionStat } from './NoteReaction';

describe('笔记互动图标', () => {
  it('点赞选中时使用心形、红色状态类和当前数字', () => {
    const html = renderToStaticMarkup(<NoteReactionButton kind="like" active count={12}
      disabled={false} busy={false} onClick={() => {}} />);
    expect(html).toContain('is-like is-active');
    expect(html).toContain('aria-label="取消点赞，当前 12"');
    expect(html).toContain('aria-pressed="true"');
    expect(html).toContain('fill="currentColor"');
    expect(html).toContain('>12<');
  });

  it('收藏未选中时使用空心星形，卡片统计保留可访问名称', () => {
    const button = renderToStaticMarkup(<NoteReactionButton kind="favorite" active={false} count={3}
      disabled={false} busy={false} onClick={() => {}} />);
    const stat = renderToStaticMarkup(<NoteReactionStat kind="comment" count={5} />);
    expect(button).toContain('aria-label="收藏，当前 3"');
    expect(button).toContain('fill="none"');
    expect(stat).toContain('aria-label="评论 5"');
  });

  it('列表统计能体现当前账号已点赞和已收藏', () => {
    const liked = renderToStaticMarkup(<NoteReactionStat kind="like" count={8} active />);
    const favorited = renderToStaticMarkup(<NoteReactionStat kind="favorite" count={3} active />);
    expect(liked).toContain('is-like is-active');
    expect(liked).toContain('aria-label="已点赞 8"');
    expect(liked).toContain('fill="currentColor"');
    expect(favorited).toContain('is-favorite is-active');
    expect(favorited).toContain('aria-label="已收藏 3"');
  });

  it('评论使用与点赞收藏相同的按钮结构并提供当前数量', () => {
    const html = renderToStaticMarkup(<NoteReactionButton kind="comment" count={5} onClick={() => {}} />);
    expect(html).toContain('note-reaction-button is-comment');
    expect(html).toContain('aria-label="查看评论，当前 5"');
    expect(html).not.toContain('aria-pressed');
  });
});
