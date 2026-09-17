import { useEffect, useRef, useState } from 'react';
import { type ProgressEvent, type ProgressReport, type Range } from '../api';
import { ProgressQueue, watchedRange } from './progress';
import { useLearning } from './LearningContext';
import { formatDuration } from './formatDuration';

export function usePlayback() {
  const { playing, apiRequest } = useLearning();
  const chapter = playing!.chapter;
  const session = playing!.session;
  const videoRef = useRef<HTMLVideoElement>(null);
  const [status, setStatus] = useState('等待播放');
  const [mediaError, setMediaError] = useState(false);
  const [progressFeedback, setProgressFeedback] = useState<'normal' | 'retrying' | 'blocked'>('normal');
  useEffect(() => {
    const video = videoRef.current;
    if (!video) return;
    let sequence = 0;
    let previousMs: number | null = null;
    let previousTime = performance.now();
    let ranges: Range[] = [];
    let restored = false;
    let restoreStarted = false;
    let lastReportedPositionMs: number | null = null;
    let active = true;
    const queue = new ProgressQueue(async event => {
      const result = await apiRequest<ProgressReport>(
        `/api/learning/videos/${encodeURIComponent(chapter.videoId)}/progress`, event);
      if (!result.accepted) throw new Error('服务器未确认接收');
    }, (message, feedback = 'normal') => {
      if (active) { setStatus(message); setProgressFeedback(feedback); }
    });
    const reset = () => { previousMs = null; previousTime = performance.now(); };
    const report = (eventType: ProgressEvent['eventType']) => {
      if (!restored || video.seeking) return;
      const positionMs = Math.min(chapter.videoDurationMs, Math.max(0, Math.round(video.currentTime * 1000)));
      if (eventType === 'HEARTBEAT' && positionMs === lastReportedPositionMs && ranges.length === 0) return;
      const queued = queue.push({ eventId: crypto.randomUUID(), sessionId: session.sessionId,
        sessionEpoch: session.sessionEpoch, sequence: ++sequence,
        eventType, positionMs, playedRanges: ranges, clientOccurredAt: new Date().toISOString(),
        playbackRate: video.playbackRate });
      if (queued) { ranges = []; lastReportedPositionMs = positionMs; }
      else if (!video.paused) video.pause();
    };
    const sample = () => {
      const currentMs = Math.min(chapter.videoDurationMs, Math.round(video.currentTime * 1000));
      const now = performance.now();
      if (restored && !video.paused && !video.seeking && video.readyState >= 3) {
        const range = watchedRange(previousMs, currentMs, now - previousTime, video.playbackRate);
        if (range) {
          const last = ranges.at(-1);
          if (last && range.startMs === last.endMs) last.endMs = range.endMs;
          else ranges.push(range);
          if (ranges.length >= 20 || ranges.reduce((sum, item) => sum + item.endMs - item.startMs, 0) >= 20000) report('HEARTBEAT');
        }
        previousMs = currentMs;
      } else previousMs = null;
      previousTime = now;
    };
    const finishRestore = () => {
      restored = true;
      reset();
      setStatus(`已恢复至 ${formatDuration(video.currentTime * 1000)}`);
    };
    const metadata = () => {
      if (restoreStarted) return;
      restoreStarted = true;
      video.currentTime = Math.min(session.resumePositionMs / 1000, Number.isFinite(video.duration) ? video.duration : 0);
      // Initial seeking is restoration, not a user action; never persist its intermediate position.
      if (!video.seeking) finishRestore();
    };
    const pause = () => { report('PAUSE'); reset(); };
    const seeking = () => { reset(); };
    const seeked = () => {
      if (restoreStarted && !restored) finishRestore();
      else { report('SEEKED'); reset(); }
    };
    const ended = () => { report('ENDED'); reset(); };
    const hidden = () => { if (document.visibilityState === 'hidden') report('EXIT'); };
    const mediaFailed = () => setMediaError(true);
    video.addEventListener('loadedmetadata', metadata);
    video.addEventListener('timeupdate', sample);
    video.addEventListener('pause', pause);
    video.addEventListener('seeking', seeking);
    video.addEventListener('seeked', seeked);
    video.addEventListener('ended', ended);
    video.addEventListener('waiting', reset);
    video.addEventListener('ratechange', reset);
    video.addEventListener('error', mediaFailed);
    document.addEventListener('visibilitychange', hidden);
    const heartbeat = window.setInterval(() => { if (!video.paused) report('HEARTBEAT'); }, 10000);
    const retry = window.setInterval(() => void queue.flush(), 5000);
    if (video.readyState >= 1) metadata();
    return () => {
      report('EXIT');
      active = false;
      queue.stop();
      window.clearInterval(heartbeat);
      window.clearInterval(retry);
      video.removeEventListener('loadedmetadata', metadata);
      video.removeEventListener('timeupdate', sample);
      video.removeEventListener('pause', pause);
      video.removeEventListener('seeking', seeking);
      video.removeEventListener('seeked', seeked);
      video.removeEventListener('ended', ended);
      video.removeEventListener('waiting', reset);
      video.removeEventListener('ratechange', reset);
      video.removeEventListener('error', mediaFailed);
      document.removeEventListener('visibilitychange', hidden);
      // One final submission is attempted; unload delivery is not guaranteed.
    };
  }, [chapter.videoId, chapter.videoDurationMs, session.sessionId, session.sessionEpoch, session.resumePositionMs, apiRequest]);

  return { chapter, videoRef, status, mediaError, progressFeedback };
}
