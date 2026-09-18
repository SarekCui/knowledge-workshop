const formatter = new Intl.DateTimeFormat('zh-CN', {
  timeZone: 'Asia/Shanghai',
  year: 'numeric',
  month: 'short',
  day: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
  hour12: false,
});

const ISO_WITH_OFFSET = /(?:Z|[+-]\d{2}:?\d{2})$/i;

function parseServerTime(value: string) {
  // Note 服务当前以 UTC LocalDateTime 输出无偏移时间；补 Z 后按真实 UTC 时刻解析，
  // 再由 formatter 统一转换为北京时间。多地区版本再改为服务端显式 OffsetDateTime。
  return new Date(ISO_WITH_OFFSET.test(value) ? value : `${value}Z`);
}

export function formatNoteDate(value?: string | null) {
  if (!value) return '尚未发布';
  const date = parseServerTime(value);
  return Number.isNaN(date.getTime()) ? '时间未知' : formatter.format(date);
}
