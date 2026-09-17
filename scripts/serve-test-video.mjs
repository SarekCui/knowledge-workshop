import { createServer } from 'node:http';
import { createReadStream } from 'node:fs';
import { stat } from 'node:fs/promises';
import { basename, resolve } from 'node:path';
import { pathToFileURL } from 'node:url';

// Single-range requests only. null means full response; false means unsatisfiable.
export function parseRange(header, size) {
  if (!header) return null;
  const match = /^bytes=(\d*)-(\d*)$/.exec(header);
  if (!match || (!match[1] && !match[2])) return false;
  let start;
  let end;
  if (!match[1]) {
    const suffix = Number(match[2]);
    if (!Number.isSafeInteger(suffix) || suffix <= 0) return false;
    start = Math.max(0, size - suffix);
    end = size - 1;
  } else {
    start = Number(match[1]);
    end = match[2] ? Number(match[2]) : size - 1;
  }
  if (!Number.isSafeInteger(start) || !Number.isSafeInteger(end) || start >= size || start > end) return false;
  return { start, end: Math.min(end, size - 1) };
}

export async function startVideoServer(file, port = 8090) {
  const videoFile = resolve(file);
  const info = await stat(videoFile);
  if (!info.isFile() || info.size === 0 || !videoFile.toLowerCase().endsWith('.mp4')) {
    throw new Error('请提供有效的非空MP4文件');
  }
  const route = `/${encodeURIComponent(basename(videoFile))}`;
  const server = createServer((request, response) => {
    // No directory listing, uploads or access to any other local file.
    if (request.url?.split('?')[0] !== route) {
      response.writeHead(404).end(); return;
    }
    if (!['GET', 'HEAD'].includes(request.method)) {
      response.writeHead(405, { Allow: 'GET, HEAD' }).end(); return;
    }
    const range = parseRange(request.headers.range, info.size);
    if (range === false) {
      response.writeHead(416, { 'Content-Range': `bytes */${info.size}` }).end(); return;
    }
    const start = range?.start ?? 0;
    const end = range?.end ?? info.size - 1;
    response.writeHead(range ? 206 : 200, {
      'Content-Type': 'video/mp4',
      'Accept-Ranges': 'bytes',
      'Content-Length': end - start + 1,
      'Cache-Control': 'no-store',
      'X-Content-Type-Options': 'nosniff',
      ...(range ? { 'Content-Range': `bytes ${start}-${end}/${info.size}` } : {}),
    });
    if (request.method === 'HEAD') { response.end(); return; }
    const stream = createReadStream(videoFile, { start, end });
    stream.on('error', () => response.destroy());
    response.on('close', () => stream.destroy());
    stream.pipe(response);
  });
  await new Promise((accept, reject) => {
    server.once('error', reject);
    server.listen(port, '127.0.0.1', accept);
  });
  return { server, url: `http://127.0.0.1:${server.address().port}${route}` };
}

if (process.argv[1] && import.meta.url === pathToFileURL(resolve(process.argv[1])).href) {
  if (!process.argv[2]) throw new Error('用法: node scripts/serve-test-video.mjs /absolute/path/video.mp4');
  const { server, url } = await startVideoServer(process.argv[2]);
  console.log(`测试视频地址: ${url}`);
  console.log('仅供本机测试，不修改原文件、不开放其他下载目录文件。');
  for (const signal of ['SIGINT', 'SIGTERM']) process.on(signal, () => server.close(() => process.exit(0)));
}
