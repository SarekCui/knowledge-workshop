import { renderToStaticMarkup } from 'react-dom/server';
import { describe, expect, it } from 'vitest';
import { MarkdownDocument } from './MarkdownDocument';

describe('MarkdownDocument', () => {
  it('渲染GFM、目录与带语言的代码块', () => {
    const html = renderToStaticMarkup(<MarkdownDocument showToc
      content={'# Redis\n## 原子操作\n|命令|用途|\n|-|-|\n|SETNX|占位|\n```java\nString key = "lock";\n```'} />);
    expect(html).toContain('aria-label="文章目录"');
    expect(html).toContain('href="#原子操作"');
    expect(html).toContain('<table>');
    expect(html).toContain('language-java');
  });

  it('不执行原始HTML并保护外部链接', () => {
    const html = renderToStaticMarkup(<MarkdownDocument
      content={'<script>alert(1)</script>\n[官方文档](https://example.com)'} />);
    expect(html).not.toContain('<script>');
    expect(html).not.toContain('alert(1)');
    expect(html).toContain('target="_blank"');
    expect(html).toContain('rel="noreferrer noopener"');
  });
});
