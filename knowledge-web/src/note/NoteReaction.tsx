import { Button } from 'antd';
import { CommentOutlined } from '@ant-design/icons';

type ReactionKind = 'like' | 'favorite' | 'comment';

function ReactionIcon({ kind, active = false }: { kind: ReactionKind; active?: boolean }) {
  if (kind === 'like') return <svg viewBox="0 0 24 24" aria-hidden="true">
    <path d="M12 20.3 4.3 13A5.1 5.1 0 0 1 11.5 5.8l.5.5.5-.5A5.1 5.1 0 0 1 19.7 13Z"
      fill={active ? 'currentColor' : 'none'} stroke="currentColor" strokeWidth="1.8" strokeLinejoin="round" />
  </svg>;
  if (kind === 'favorite') return <svg viewBox="0 0 24 24" aria-hidden="true">
    <path d="m12 3.4 2.65 5.37 5.93.86-4.29 4.18 1.02 5.9L12 16.92l-5.31 2.79 1.02-5.9-4.29-4.18 5.93-.86Z"
      fill={active ? 'currentColor' : 'none'} stroke="currentColor" strokeWidth="1.8" strokeLinejoin="round" />
  </svg>;
  return <CommentOutlined aria-hidden="true" />;
}

export function NoteReactionButton({ kind, active, count, disabled, busy, onClick }: {
  kind: ReactionKind;
  active?: boolean;
  count: number;
  disabled?: boolean;
  busy?: boolean;
  onClick: () => void;
}) {
  const label = kind === 'like' ? (active ? '取消点赞' : '点赞')
    : kind === 'favorite' ? (active ? '取消收藏' : '收藏') : '查看评论';
  return <Button className={`note-reaction-button is-${kind}${active ? ' is-active' : ''}`}
    aria-label={`${label}，当前 ${count}`} aria-pressed={kind === 'comment' ? undefined : active} aria-busy={busy}
    disabled={disabled} onClick={onClick}>
    <span className="note-reaction-icon"><ReactionIcon kind={kind} active={active} /></span>
    <span className="note-reaction-count" aria-hidden="true">{count}</span>
  </Button>;
}

export function NoteReactionStat({ kind, count, active = false }: { kind: ReactionKind; count: number; active?: boolean }) {
  const label = kind === 'like' ? '点赞' : kind === 'favorite' ? '收藏' : '评论';
  return <span className={`note-reaction-stat is-${kind}${active ? ' is-active' : ''}`}
    aria-label={`${active ? `已${label}` : label} ${count}`} title={active ? `已${label}` : label}>
    <span className="note-reaction-icon"><ReactionIcon kind={kind} active={active} /></span>
    <span aria-hidden="true">{count}</span>
  </span>;
}

export function NoteReactionStatButton({ kind, count, active = false, disabled, busy, onClick }: {
  kind: Exclude<ReactionKind, 'comment'>;
  count: number;
  active?: boolean;
  disabled?: boolean;
  busy?: boolean;
  onClick: () => void;
}) {
  const label = active ? '取消点赞' : '点赞';
  return <Button type="text" className={`note-reaction-stat-button is-${kind}${active ? ' is-active' : ''}`}
    aria-label={`${label}，当前 ${count}`} aria-pressed={active} aria-busy={busy}
    disabled={disabled} onClick={onClick}>
    <span className="note-reaction-icon"><ReactionIcon kind={kind} active={active} /></span>
    <span aria-hidden="true">{count}</span>
  </Button>;
}
