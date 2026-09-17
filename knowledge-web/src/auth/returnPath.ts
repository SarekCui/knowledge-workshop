export function returnPath(candidate: unknown): string {
  return typeof candidate === 'string' && /^\/(?:account|courses(?:\/[^/]+)?|notes(?:\/(?:course\/[^/]+|[^/]+))?|points|learning(?:\/courses\/[^/]+)?|activities(?:\/[^/]+)?|orders(?:\/[^/]+)?)$/.test(candidate)
    ? candidate : '/learning';
}
