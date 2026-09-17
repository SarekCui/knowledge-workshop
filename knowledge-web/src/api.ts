export class ApiError extends Error {
  constructor(public status: number, message: string, public requestId?: string) {
    super(message);
  }
}

export interface RequestOptions { webAuth?: boolean; method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE' }
export async function request<T>(path: string, token?: string, body?: unknown, options: RequestOptions = {}): Promise<T> {
  const multipart = body instanceof FormData;
  const response = await fetch(path, {
    method: options.method ?? (body === undefined ? 'GET' : 'POST'),
    headers: {
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(body === undefined || multipart ? {} : { 'Content-Type': 'application/json' }),
      ...(options.webAuth ? { 'X-Web-Auth': '1' } : {}),
    },
    body: body === undefined ? undefined : multipart ? body : JSON.stringify(body),
    credentials: 'same-origin',
    signal: AbortSignal.timeout(15000),
  });
  const result = await response.json().catch(() => null);
  if (!response.ok) throw new ApiError(response.status,
    result?.message ?? `请求失败（HTTP ${response.status}）`, result?.requestId);
  if (!result || !('data' in result)) throw new Error('服务器响应格式不正确');
  return result.data as T;
}

export interface TokenPair { accessToken: string; refreshToken: string }
export interface Course {
  courseId: string; title: string; completionRate: number;
  totalVideos: number; completedVideos: number; videoId: string | null;
  coverUrl: string | null; resumePositionMs: number; lastLearnedAt: string | null;
}
export interface Chapter {
  id: string; title: string; videoId: string; videoUrl: string;
  videoDurationMs: number; videoVersion: number;
}
export interface Session {
  sessionId: string; sessionEpoch: number; resumePositionMs: number;
}
export interface Range { startMs: number; endMs: number }
export interface ProgressEvent {
  eventId: string; sessionId: string; sessionEpoch: number; sequence: number;
  eventType: 'HEARTBEAT' | 'PAUSE' | 'SEEKED' | 'ENDED' | 'EXIT';
  positionMs: number; playedRanges: Range[]; clientOccurredAt: string; playbackRate: number;
}
export interface ProgressReport { accepted: boolean; cacheUpdated: boolean }
