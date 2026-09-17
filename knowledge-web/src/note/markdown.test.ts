import { describe, expect, it } from 'vitest';
import { extractMarkdownHeadings, markdownExcerpt } from './markdown';

describe('技术 Note Markdown', () => {
  it('提取目录并忽略代码块里的伪标题', () => {
    const headings = extractMarkdownHeadings('# Redis 锁\n## **加锁**流程\n```md\n# 不是标题\n```\n## 加锁流程');
    expect(headings).toEqual([
      { depth: 1, id: 'redis-锁', text: 'Redis 锁' },
      { depth: 2, id: '加锁流程', text: '加锁流程' },
      { depth: 2, id: '加锁流程-1', text: '加锁流程' },
    ]);
  });

  it('卡片摘要移除Markdown并压缩代码块', () => {
    expect(markdownExcerpt('## 缓存\n**结论** [文档](https://example.com)\n```java\nclass Demo {}\n```'))
      .toBe('缓存 结论 文档 代码片段');
  });
});
