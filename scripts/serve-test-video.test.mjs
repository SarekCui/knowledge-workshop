import { test } from 'node:test';
import assert from 'node:assert/strict';
import { parseRange } from './serve-test-video.mjs';

test('没有Range时返回完整文件', () => assert.equal(parseRange(undefined, 100), null));
test('支持固定、开放结尾和后缀区间', () => {
  assert.deepEqual(parseRange('bytes=10-19', 100), { start: 10, end: 19 });
  assert.deepEqual(parseRange('bytes=90-', 100), { start: 90, end: 99 });
  assert.deepEqual(parseRange('bytes=-10', 100), { start: 90, end: 99 });
  assert.deepEqual(parseRange('bytes=90-200', 100), { start: 90, end: 99 });
});
test('越界、倒序、多区间、空区间与精度溢出拒绝', () => {
  for (const header of ['bytes=100-', 'bytes=20-10', 'bytes=0-1,5-6', 'bytes=-',
    'bytes=-0', 'bytes=9007199254740993-', 'bad']) {
    assert.equal(parseRange(header, 100), false);
  }
});
