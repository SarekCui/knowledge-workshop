import { Alert, Button, Typography } from 'antd';
import { usePlayback } from './usePlayback';
import { usePlayerControls } from './usePlayerControls';
import { useLearning } from './LearningContext';
import { formatDuration } from './formatDuration';

export function Player() {
  const { chapter, videoRef, status, mediaError, progressFeedback } = usePlayback();
  const { chapters, openVideo, busy } = useLearning();
  const controls = usePlayerControls(videoRef);
  return <div className="player-content">
    <div className="player-video-frame" tabIndex={0} role="group" aria-label="视频播放区域"
      aria-describedby="player-shortcuts" onKeyDown={controls.onKeyDown}>
    <video ref={videoRef} src={chapter.videoUrl} controls playsInline preload="metadata"
      onRateChange={controls.syncRate} aria-label={chapter.title} />
    </div>
    <div className="player-toolbar" role="group" aria-label="播放控制">
      <Button disabled={mediaError} onClick={() => controls.seek(-10)}>后退 10 秒</Button>
      <Button disabled={mediaError} onClick={() => controls.seek(10)}>前进 10 秒</Button>
      <label className="player-speed">倍速 <select aria-label="播放速度" value={controls.rate}
        onChange={event => controls.changeRate(Number(event.target.value))}>
        {[.75, 1, 1.25, 1.5, 2].map(rate => <option key={rate} value={rate}>{rate}×</option>)}
      </select></label>
      <Button onClick={() => void controls.fullscreen()}>全屏</Button>
    </div>
    <div className="player-details">
    <span className="muted">第 {chapters.findIndex(item => item.id === chapter.id) + 1} 节 · {formatDuration(chapter.videoDurationMs)}</span>
    <Typography.Title level={3}>{chapter.title}</Typography.Title>
    {progressFeedback !== 'normal' ? <Alert type="warning" showIcon message={status}
      action={progressFeedback === 'blocked' ? <Button loading={busy} onClick={() => void openVideo(chapter)}>重新打开视频</Button> : undefined} />
      : <span className="player-save-status" role="status">{status} · 自动记录学习进度</span>}
    {mediaError && <Alert type="error" showIcon message="视频暂时无法播放" description="请检查网络连接后重试。"
      action={<Button loading={busy} onClick={() => void openVideo(chapter)}>重新加载视频</Button>} />}
    {controls.controlError ? <Alert type="warning" message={controls.controlError} /> : null}
    <details className="player-shortcuts"><summary>键盘快捷键</summary>
      <p id="player-shortcuts">聚焦视频播放区域后：空格播放/暂停，← → 跳转 10 秒，↑ ↓ 调节音量，F 全屏。</p>
    </details>
    </div>
  </div>;
}
