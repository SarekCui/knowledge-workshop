import { expect, it } from 'vitest';
import { clampCropOffset, sourceCropRect } from './cropGeometry';

it('限制拖动范围且换算为原图裁剪区域', () => {
  expect(clampCropOffset({ width: 600, height: 300 }, 300, 1, { x: 999, y: 100 }))
    .toEqual({ x: 150, y: 0 });
  expect(sourceCropRect({ width: 600, height: 300 }, 300, 1, { x: 150, y: 0 }))
    .toEqual({ x: 0, y: 0, size: 300 });
});

it('缩放后允许移动并缩小原图裁剪尺寸', () => {
  const offset = clampCropOffset({ width: 300, height: 300 }, 300, 2, { x: -150, y: -150 });
  expect(offset).toEqual({ x: -150, y: -150 });
  expect(sourceCropRect({ width: 300, height: 300 }, 300, 2, offset)).toEqual({ x: 150, y: 150, size: 150 });
});
