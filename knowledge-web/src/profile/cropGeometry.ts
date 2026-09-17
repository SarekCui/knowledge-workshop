export interface CropOffset { x: number; y: number; }
export interface ImageSize { width: number; height: number; }

export function clampCropOffset(size: ImageSize, viewport: number, zoom: number, offset: CropOffset): CropOffset {
  const baseScale = Math.max(viewport / size.width, viewport / size.height);
  const width = size.width * baseScale * zoom;
  const height = size.height * baseScale * zoom;
  const maxX = Math.max(0, (width - viewport) / 2);
  const maxY = Math.max(0, (height - viewport) / 2);
  return { x: Math.max(-maxX, Math.min(maxX, offset.x)), y: Math.max(-maxY, Math.min(maxY, offset.y)) };
}

export function sourceCropRect(size: ImageSize, viewport: number, zoom: number, offset: CropOffset) {
  const baseScale = Math.max(viewport / size.width, viewport / size.height);
  const scale = baseScale * zoom;
  const displayWidth = size.width * scale;
  const displayHeight = size.height * scale;
  const left = (viewport - displayWidth) / 2 + offset.x;
  const top = (viewport - displayHeight) / 2 + offset.y;
  const x = -left / scale;
  const y = -top / scale;
  return {
    x: Object.is(x, -0) ? 0 : x,
    y: Object.is(y, -0) ? 0 : y,
    size: viewport / scale,
  };
}
