import { useState, type KeyboardEvent, type RefObject } from 'react';

export function seekVideo(video: HTMLVideoElement, seconds: number) {
  if (video.readyState < 1 || !Number.isFinite(video.duration)) return;
  video.currentTime = Math.min(video.duration, Math.max(0, video.currentTime + seconds));
}

export function usePlayerControls(videoRef: RefObject<HTMLVideoElement>) {
  const [rate, setRate] = useState(1);
  const [controlError, setControlError] = useState('');
  const seek = (seconds: number) => {
    if (videoRef.current) seekVideo(videoRef.current, seconds);
  };
  const changeRate = (value: number) => {
    if (videoRef.current) videoRef.current.playbackRate = value;
  };
  const syncRate = () => setRate(videoRef.current?.playbackRate ?? 1);
  const togglePlayback = async () => {
    const video = videoRef.current;
    if (!video) return;
    setControlError('');
    try { if (video.paused) await video.play(); else video.pause(); }
    catch { setControlError('暂时无法开始播放，请重试。'); }
  };
  const fullscreen = async () => {
    const video = videoRef.current;
    if (!video) return;
    setControlError('');
    try {
      if (document.fullscreenElement) await document.exitFullscreen();
      else if (video.requestFullscreen) await video.requestFullscreen();
      else setControlError('此浏览器请使用视频自带的全屏按钮。');
    } catch { setControlError('无法进入全屏，请使用视频自带的全屏按钮。'); }
  };
  const onKeyDown = (event: KeyboardEvent<HTMLElement>) => {
    // Native controls, buttons and editors keep their own keyboard behavior.
    if (event.target !== event.currentTarget || event.altKey || event.ctrlKey || event.metaKey) return;
    const video = videoRef.current;
    if (!video) return;
    switch (event.key.toLowerCase()) {
      case ' ': event.preventDefault(); void togglePlayback(); break;
      case 'arrowleft': event.preventDefault(); seek(-10); break;
      case 'arrowright': event.preventDefault(); seek(10); break;
      case 'arrowup': event.preventDefault(); video.muted = false; video.volume = Math.min(1, video.volume + .1); break;
      case 'arrowdown': event.preventDefault(); video.volume = Math.max(0, video.volume - .1); break;
      case 'f': event.preventDefault(); void fullscreen(); break;
    }
  };
  return { rate, controlError, seek, changeRate, syncRate, fullscreen, onKeyDown };
}
