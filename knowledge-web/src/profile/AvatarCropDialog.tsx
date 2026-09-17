import { Button, Modal, Slider, Space } from 'antd';
import { useEffect, useRef, useState, type PointerEvent as ReactPointerEvent } from 'react';
import { useProfile } from './ProfileContext';
import { useAvatarCrop } from './AvatarCropContext';
import { clampCropOffset, sourceCropRect, type CropOffset, type ImageSize } from './cropGeometry';

const VIEWPORT = 300;
const OUTPUT = 256;

export function AvatarCropDialog() {
  const crop = useAvatarCrop();
  const { busy, store } = useProfile();
  const imageRef = useRef<HTMLImageElement>(null);
  const dragRef = useRef<{ pointerId: number; startX: number; startY: number; origin: CropOffset } | null>(null);
  const [size, setSize] = useState<ImageSize>({ width: 1, height: 1 });
  const [zoom, setZoom] = useState(1);
  const [offset, setOffset] = useState<CropOffset>({ x: 0, y: 0 });

  useEffect(() => { setZoom(1); setOffset({ x: 0, y: 0 }); }, [crop.sourceUrl]);
  const displayScale = Math.max(VIEWPORT / size.width, VIEWPORT / size.height);
  const startDrag = (event: ReactPointerEvent<HTMLDivElement>) => {
    event.currentTarget.setPointerCapture(event.pointerId);
    dragRef.current = { pointerId: event.pointerId, startX: event.clientX, startY: event.clientY, origin: offset };
  };
  const drag = (event: ReactPointerEvent<HTMLDivElement>) => {
    const active = dragRef.current;
    if (!active || active.pointerId !== event.pointerId) return;
    setOffset(clampCropOffset(size, VIEWPORT, zoom, {
      x: active.origin.x + event.clientX - active.startX,
      y: active.origin.y + event.clientY - active.startY,
    }));
  };
  const stopDrag = () => { dragRef.current = null; };
  const changeZoom = (next: number) => {
    setZoom(next);
    setOffset(current => clampCropOffset(size, VIEWPORT, next, current));
  };
  const confirm = async () => {
    const image = imageRef.current;
    if (!image || !crop.sourceUrl) return;
    const rect = sourceCropRect(size, VIEWPORT, zoom, offset);
    const canvas = document.createElement('canvas');
    canvas.width = OUTPUT; canvas.height = OUTPUT;
    const context = canvas.getContext('2d');
    if (!context) return;
    context.fillStyle = '#ffffff'; context.fillRect(0, 0, OUTPUT, OUTPUT);
    context.drawImage(image, rect.x, rect.y, rect.size, rect.size, 0, 0, OUTPUT, OUTPUT);
    const blob = await new Promise<Blob | null>(resolve => canvas.toBlob(resolve, 'image/jpeg', 0.9));
    if (!blob) return;
    await store.uploadAvatar(new File([blob], 'avatar.jpg', { type: 'image/jpeg' }));
    if (!store.getSnapshot().error) crop.close();
  };

  return <Modal title="裁剪头像" open={Boolean(crop.sourceUrl)} onCancel={busy ? undefined : crop.close}
    footer={<Space><Button disabled={busy} onClick={crop.close}>取消</Button>
      <Button type="primary" loading={busy} onClick={() => void confirm()}>保存头像</Button></Space>}>
    <div className="avatar-crop-stage" role="application" aria-label="拖动图片调整头像显示范围"
      onPointerDown={startDrag} onPointerMove={drag} onPointerUp={stopDrag} onPointerCancel={stopDrag}>
      {crop.sourceUrl ? <img ref={imageRef} src={crop.sourceUrl} alt="头像裁剪预览" draggable={false}
        onLoad={event => {
          const next = { width: event.currentTarget.naturalWidth, height: event.currentTarget.naturalHeight };
          setSize(next); setOffset({ x: 0, y: 0 });
        }} style={{ width: size.width * displayScale, height: size.height * displayScale,
          transform: `translate(calc(-50% + ${offset.x}px), calc(-50% + ${offset.y}px)) scale(${zoom})` }} /> : null}
      <div className="avatar-crop-mask" aria-hidden="true" />
    </div>
    <label className="avatar-zoom"><span>缩放</span><Slider min={1} max={3} step={0.01} value={zoom} onChange={changeZoom} /></label>
    <p className="muted">拖动图片选择显示范围，使用滑块缩放。最终上传 256 × 256 的 JPEG 图片。</p>
  </Modal>;
}
