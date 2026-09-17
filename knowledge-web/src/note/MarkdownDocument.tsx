import { useMemo } from 'react';
import hljs from 'highlight.js/lib/core';
import bash from 'highlight.js/lib/languages/bash';
import css from 'highlight.js/lib/languages/css';
import java from 'highlight.js/lib/languages/java';
import javascript from 'highlight.js/lib/languages/javascript';
import json from 'highlight.js/lib/languages/json';
import markdown from 'highlight.js/lib/languages/markdown';
import python from 'highlight.js/lib/languages/python';
import sql from 'highlight.js/lib/languages/sql';
import typescript from 'highlight.js/lib/languages/typescript';
import xml from 'highlight.js/lib/languages/xml';
import yaml from 'highlight.js/lib/languages/yaml';
import ReactMarkdown, { type Components } from 'react-markdown';
import rehypeSanitize from 'rehype-sanitize';
import rehypeSlug from 'rehype-slug';
import remarkGfm from 'remark-gfm';
import 'highlight.js/styles/github-dark.css';
import { extractMarkdownHeadings } from './markdown';
import { AuthenticatedNoteImage } from './AuthenticatedNoteImage';

const languages = { bash, css, java, javascript, json, markdown, python, sql, typescript, xml, yaml };
for (const [name, grammar] of Object.entries(languages)) hljs.registerLanguage(name, grammar);
hljs.registerAliases(['sh', 'shell'], { languageName: 'bash' });
hljs.registerAliases(['js'], { languageName: 'javascript' });
hljs.registerAliases(['ts'], { languageName: 'typescript' });
hljs.registerAliases(['html'], { languageName: 'xml' });

const markdownComponents: Components = {
  a({ href, children, ...props }) {
    const external = href?.startsWith('https://') || href?.startsWith('http://');
    return <a href={href} {...props} target={external ? '_blank' : undefined}
      rel={external ? 'noreferrer noopener' : undefined}>{children}</a>;
  },
  code({ className, children, ...props }) {
    const language = /language-([\w-]+)/.exec(className ?? '')?.[1];
    if (!language || !hljs.getLanguage(language)) return <code className={className} {...props}>{children}</code>;
    const highlighted = hljs.highlight(String(children).replace(/\n$/, ''), { language }).value;
    return <code className={`${className ?? ''} hljs`} {...props}
      dangerouslySetInnerHTML={{ __html: highlighted }} />;
  },
  img({ src, alt }) {
    return <AuthenticatedNoteImage src={src} alt={alt} />;
  },
};

export function MarkdownDocument({ content, showToc = false }: { content: string; showToc?: boolean }) {
  const headings = useMemo(() => showToc ? extractMarkdownHeadings(content) : [], [content, showToc]);
  return <div className="markdown-document">
    {headings.length >= 2 ? <nav className="markdown-toc" aria-label="文章目录">
      <strong>目录</strong>
      <ol>{headings.map(heading => <li key={heading.id} className={`toc-level-${heading.depth}`}>
        <a href={`#${heading.id}`}>{heading.text}</a>
      </li>)}</ol>
    </nav> : null}
    <div className="markdown-body">
      <ReactMarkdown remarkPlugins={[remarkGfm]}
        rehypePlugins={[rehypeSanitize, rehypeSlug]}
        components={markdownComponents}>{content}</ReactMarkdown>
    </div>
  </div>;
}
