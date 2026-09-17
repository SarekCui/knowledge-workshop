import { renderToStaticMarkup } from 'react-dom/server';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { NotePanel } from './NotePanel';

const state = vi.hoisted(() => ({ notes: [], status: 'success', busy: false, error: '', notice: '',
  draft: undefined, keyword: '', searchInput: '', store: { load: () => {}, create: () => {},
    setSearchInput: () => {}, search: () => {}, clearSearch: () => {} } }));
vi.mock('./NoteContext', () => ({ useNotes: () => state }));

describe('Note搜索展示', () => {
  beforeEach(() => { state.status = 'success'; state.keyword = ''; state.error = ''; });
  it('搜索成功为空不推断用户没有Note', () => {
    state.keyword = 'Redis';
    const html = renderToStaticMarkup(<NotePanel />);
    expect(html).toContain('没有找到匹配名称的 Note');
    expect(html).toContain('清除搜索');
    expect(html).not.toContain('还没有 Note');
  });
  it('请求失败显示错误而不是无结果', () => {
    state.status = 'error'; state.error = '服务不可用';
    const html = renderToStaticMarkup(<NotePanel />);
    expect(html).toContain('服务不可用');
    expect(html).not.toContain('没有找到匹配名称');
    expect(html).not.toContain('还没有 Note');
  });
  it('加载时不显示空搜索结果', () => {
    state.status = 'loading'; state.keyword = 'Redis';
    const html = renderToStaticMarkup(<NotePanel />);
    expect(html).toContain('正在加载 Note');
    expect(html).not.toContain('没有找到匹配名称');
  });
});
