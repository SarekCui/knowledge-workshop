import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

test('中文课程修复SQL显式声明UTF-8连接编码', async () => {
  const sql = await readFile(new URL('./repair-demo-course.sql', import.meta.url), 'utf8');
  assert.match(sql, /SET NAMES utf8mb4;/);
});
