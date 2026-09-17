import { describe, expect, it, vi } from 'vitest';
import { CourseCatalogStore, type CatalogTransport } from './CourseCatalogStore';
import type { CatalogCourse } from './types';

const course: CatalogCourse = {
  id: 'course-java', title: 'Java 并发实战', summary: '从线程基础到分布式锁', coverUrl: null,
  priceCents: 9900, categoryId: 'backend', categoryName: '后端开发', chapterCount: 8,
  totalDurationMs: 3_600_000, entitled: true, updatedAt: '2026-09-16T00:00:00',
};
const page = (items: CatalogCourse[]) => ({ items, pageNo: 1, pageSize: 12, total: items.length });

describe('课程目录状态', () => {
  it('初次进入并发加载分类和课程，不创建前端假数据', async () => {
    const transport = vi.fn(async (path: string) => path.endsWith('/categories')
      ? [{ id: 'backend', name: '后端开发' }] : page([course]));
    const store = new CourseCatalogStore({ kind: 'list' }, transport as CatalogTransport);
    await store.load();
    expect(transport.mock.calls.map(call => call[0])).toEqual([
      '/api/learning/catalog/categories', '/api/learning/catalog/courses?pageNo=1&pageSize=12',
    ]);
    expect(store.getSnapshot()).toMatchObject({ status: 'success', courses: [course], total: 1 });
  });

  it('分类和关键词经过 URLSearchParams 编码后共同生效', async () => {
    const transport = vi.fn(async (path: string) => path.endsWith('/categories') ? [] : page([course]));
    const store = new CourseCatalogStore({ kind: 'list' }, transport as CatalogTransport);
    await store.load();
    await store.selectCategory('backend/java');
    store.setKeyword('并发 锁');
    await store.search();
    expect(transport.mock.calls.at(-1)?.[0]).toBe(
      '/api/learning/catalog/courses?pageNo=1&pageSize=12&categoryId=backend%2Fjava&keyword=%E5%B9%B6%E5%8F%91+%E9%94%81',
    );
  });

  it('课程详情只使用安全目录接口', async () => {
    const detail = { course, chapters: [{ id: 'chapter-1', title: '第一章', sortOrder: 1, videoDurationMs: 60000 }] };
    const transport = vi.fn(async () => detail);
    const store = new CourseCatalogStore({ kind: 'detail', courseId: 'course/java' }, transport as CatalogTransport);
    await store.load();
    expect(transport).toHaveBeenCalledWith('/api/learning/catalog/courses/course%2Fjava');
    expect(store.getSnapshot().detail).toEqual(detail);
    expect(store.getSnapshot().detail?.chapters[0]).not.toHaveProperty('videoUrl');
  });
});
