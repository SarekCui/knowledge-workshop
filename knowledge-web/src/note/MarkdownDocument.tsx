import {
  isValidElement,
  useEffect,
  useMemo,
  useRef,
  useState,
  type MouseEvent as ReactMouseEvent,
  type ReactNode,
} from 'react';
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
import { CheckOutlined, CopyOutlined } from '@ant-design/icons';
import { extractMarkdownHeadings } from './markdown';
import { AuthenticatedNoteImage } from './AuthenticatedNoteImage';

const languages = { bash, css, java, javascript, json, markdown, python, sql, typescript, xml, yaml };
for (const [name, grammar] of Object.entries(languages)) hljs.registerLanguage(name, grammar);
hljs.registerAliases(['sh', 'shell'], { languageName: 'bash' });
hljs.registerAliases(['js'], { languageName: 'javascript' });
hljs.registerAliases(['ts'], { languageName: 'typescript' });
hljs.registerAliases(['html'], { languageName: 'xml' });

/** 顶部固定头高度 + 视觉留白，用于滚动高亮与锚点定位。 */
const HEADING_SCROLL_OFFSET = 96;

async function copyToClipboard(text: string) {
  if (navigator.clipboard?.writeText) {
    await navigator.clipboard.writeText(text);
    return;
  }
  const textarea = document.createElement('textarea');
  textarea.value = text;
  textarea.setAttribute('readonly', '');
  textarea.style.position = 'fixed';
  textarea.style.opacity = '0';
  document.body.appendChild(textarea);
  textarea.select();
  document.execCommand('copy');
  textarea.remove();
}

function CodeBlock({ language, children }: { language: string; children: ReactNode }) {
  const [copied, setCopied] = useState(false);
  const preRef = useRef<HTMLPreElement>(null);
  const onCopy = async () => {
    const text = preRef.current?.textContent ?? '';
    if (!text) return;
    try {
      await copyToClipboard(text);
    } catch {
      return;
    }
    setCopied(true);
    window.setTimeout(() => setCopied(false), 1800);
  };
  return <div className="code-block">
    <div className="code-block-bar">
      <span className="code-block-language">{language}</span>
      <button type="button" className={`code-copy-button${copied ? ' is-copied' : ''}`}
        aria-label={copied ? '代码已复制' : '复制代码'}
        onClick={() => void onCopy()}>{copied ? <CheckOutlined /> : <CopyOutlined />}</button>
    </div>
    <pre ref={preRef}>{children}</pre>
  </div>;
}

const LABEL_CASES: Record<string, string> = {
  java: 'Java', python: 'Python', javascript: 'JavaScript', typescript: 'TypeScript',
  sql: 'SQL', bash: 'Bash', shell: 'Shell', json: 'JSON', xml: 'XML', html: 'HTML',
  css: 'CSS', yaml: 'YAML', markdown: 'Markdown',
};

function codeLanguage(children: ReactNode): string {
  const child = Array.isArray(children) ? children[0] : children;
  if (!isValidElement(child)) return '';
  const className = (child.props as { className?: unknown } | null)?.className;
  const raw = typeof className === 'string' ? /language-([\w-]+)/.exec(className)?.[1] ?? '' : '';
  return LABEL_CASES[raw.toLowerCase()] ?? raw;
}

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
  pre({ children }) {
    return <CodeBlock language={codeLanguage(children)}>{children}</CodeBlock>;
  },
  img({ src, alt }) {
    return <AuthenticatedNoteImage src={src} alt={alt} />;
  },
};

export function MarkdownDocument({ content, showToc = false }: {
  content: string;
  showToc?: boolean;
}) {
  const headings = useMemo(() => showToc ? extractMarkdownHeadings(content) : [], [content, showToc]);
  const hasToc = showToc && headings.length >= 2;
  const [activeId, setActiveId] = useState<string | null>(null);
  const bodyRef = useRef<HTMLDivElement>(null);
  const headingElementsRef = useRef<Map<string, HTMLElement>>(new Map());

  useEffect(() => {
    if (!hasToc) {
      setActiveId(null);
      return;
    }
    const root = bodyRef.current;
    if (!root) return;
    const ids = new Set(headings.map(heading => heading.id));
    const elements = Array.from(root.querySelectorAll<HTMLElement>('h1[id], h2[id], h3[id], h4[id], h5[id], h6[id]'))
      .filter(element => ids.has(element.id));
    if (elements.length === 0) return;
    headingElementsRef.current = new Map(elements.map(element => [element.id, element]));
    const update = () => {
      const line = (window.scrollY ?? 0) + HEADING_SCROLL_OFFSET;
      let current: string | null = null;
      for (const element of elements) {
        if (element.getBoundingClientRect().top + (window.scrollY ?? 0) <= line) current = element.id;
        else break;
      }
      setActiveId(current);
    };
    update();
    window.addEventListener('scroll', update, { passive: true });
    window.addEventListener('resize', update);
    return () => {
      window.removeEventListener('scroll', update);
      window.removeEventListener('resize', update);
    };
  }, [hasToc, headings, content]);

  const jumpTo = (id: string) => (event: ReactMouseEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    headingElementsRef.current.get(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  };

  return <div className={`markdown-document${hasToc ? ' has-toc' : ''}`}>
    {hasToc ? <nav className="markdown-toc" aria-label="文章目录">
      <div className="markdown-toc-heading"><span>本页目录</span><small>{headings.length} 节</small></div>
      <ol>{headings.map(heading => <li key={heading.id} className={`toc-level-${heading.depth}`}>
        <a href={`#${heading.id}`} className={activeId === heading.id ? 'is-active' : undefined}
          aria-current={activeId === heading.id ? 'true' : undefined}
          onClick={jumpTo(heading.id)}>{heading.text}</a>
      </li>)}</ol>
    </nav> : null}
    <div className="markdown-body" ref={bodyRef}>
      <ReactMarkdown remarkPlugins={[remarkGfm]}
        rehypePlugins={[rehypeSanitize, rehypeSlug]}
        components={markdownComponents}>{content}</ReactMarkdown>
    </div>
  </div>;
}
