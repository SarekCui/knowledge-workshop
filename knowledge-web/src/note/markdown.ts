import GithubSlugger from 'github-slugger';

export interface MarkdownHeading {
  depth: number;
  id: string;
  text: string;
}

const ATX_HEADING = /^(#{1,6})\s+(.+?)\s*#*$/;
const FENCE = /^\s*(```|~~~)/;

function plainText(value: string) {
  return value
    .replace(/!\[([^\]]*)\]\([^)]*\)/g, '$1')
    .replace(/\[([^\]]+)\]\([^)]*\)/g, '$1')
    .replace(/[`*_~]/g, '')
    .trim();
}

export function extractMarkdownHeadings(markdown: string): MarkdownHeading[] {
  const headings: MarkdownHeading[] = [];
  const slugger = new GithubSlugger();
  let fenceMarker = '';
  for (const line of markdown.split(/\r?\n/)) {
    const fence = line.match(FENCE)?.[1];
    if (fence) {
      if (!fenceMarker) fenceMarker = fence;
      else if (fence === fenceMarker) fenceMarker = '';
      continue;
    }
    if (fenceMarker) continue;
    const match = line.match(ATX_HEADING);
    if (!match) continue;
    const text = plainText(match[2]);
    if (text) headings.push({ depth: match[1].length, id: slugger.slug(text), text });
  }
  return headings;
}

export function markdownExcerpt(markdown: string, maxLength = 240) {
  const plain = markdown
    .replace(/```[\s\S]*?```/g, ' 代码片段 ')
    .replace(/~~~[\s\S]*?~~~/g, ' 代码片段 ')
    .replace(/!\[([^\]]*)\]\([^)]*\)/g, '$1')
    .replace(/\[([^\]]+)\]\([^)]*\)/g, '$1')
    .replace(/^#{1,6}\s+/gm, '')
    .replace(/^\s*[-*>+]\s+/gm, '')
    .replace(/[`*_~]/g, '')
    .replace(/\s+/g, ' ')
    .trim();
  return plain.length > maxLength ? `${plain.slice(0, maxLength).trimEnd()}…` : plain;
}
