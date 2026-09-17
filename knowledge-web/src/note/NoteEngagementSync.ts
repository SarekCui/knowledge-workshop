import type { Note, NoteTransport } from './NoteStore';

export interface SynchronizedNoteEngagement {
  likeCount: number;
  favoriteCount: number;
  commentCount: number;
  liked: boolean;
  favorited: boolean;
}

interface Entry extends SynchronizedNoteEngagement { updatedAt: number }
interface Channel {
  entries: Map<string, Entry>;
  listeners: Set<(noteId: string, engagement: SynchronizedNoteEngagement) => void>;
}

const channels = new WeakMap<NoteTransport, Channel>();
const RECENT_WRITE_MS = 30_000;

function channel(transport: NoteTransport) {
  let value = channels.get(transport);
  if (!value) {
    value = { entries: new Map(), listeners: new Set() };
    channels.set(transport, value);
  }
  return value;
}

export function synchronizeNoteEngagement(transport: NoteTransport, noteId: string,
  engagement: SynchronizedNoteEngagement) {
  const current = channel(transport);
  current.entries.set(noteId, { ...engagement, updatedAt: Date.now() });
  current.listeners.forEach(listener => listener(noteId, engagement));
}

export function mergeSynchronizedEngagement(transport: NoteTransport, note: Note): Note {
  const engagement = channel(transport).entries.get(note.id);
  if (!engagement || Date.now() - engagement.updatedAt > RECENT_WRITE_MS) return note;
  return { ...note, likeCount: engagement.likeCount, favoriteCount: engagement.favoriteCount,
    commentCount: engagement.commentCount, liked: engagement.liked, favorited: engagement.favorited };
}

export function subscribeNoteEngagement(transport: NoteTransport,
  listener: (noteId: string, engagement: SynchronizedNoteEngagement) => void) {
  const current = channel(transport);
  current.listeners.add(listener);
  return () => current.listeners.delete(listener);
}
