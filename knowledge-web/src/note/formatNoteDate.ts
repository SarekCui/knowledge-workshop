const formatter = new Intl.DateTimeFormat('zh-CN', {
  year: 'numeric',
  month: 'short',
  day: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
  hour12: false,
});

export function formatNoteDate(value?: string | null) {
  if (!value) return '尚未发布';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? '时间未知' : formatter.format(date);
}
