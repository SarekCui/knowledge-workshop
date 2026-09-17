import type { RequestOptions } from '../api';

export interface SignIn { userId: string; signDate: string; continuousDays: number; rewardPoints: number; duplicated: boolean }
export interface RankingItem { userId: string; score: number; rank: number }
export interface SignInMonth { month: string; today: string; signedDates: string[] }
type Transport = <T>(path: string, body?: unknown, options?: RequestOptions) => Promise<T>;
interface Snapshot {
  season: string; ranking: RankingItem[]; loading: boolean; rankingError: string;
  signing: boolean; signError: string; signIn?: SignIn;
  month: string; calendar?: SignInMonth; calendarLoading: boolean; calendarError: string;
}
export function utcSeason(date: Date): string {
  return `${date.getUTCFullYear()}-Q${Math.floor(date.getUTCMonth() / 3) + 1}`;
}
export class PointsStore {
  private snapshot: Snapshot;
  private listeners = new Set<() => void>();
  private rankingGeneration = 0;
  private lifecycle = 0;
  private calendarGeneration = 0;
  constructor(private transport: Transport, private now: () => Date = () => new Date()) {
    this.snapshot = { season: utcSeason(now()), ranking: [], loading: false, rankingError: '', signing: false, signError: '',
      month: now().toISOString().slice(0, 7), calendarLoading: false, calendarError: '' };
  }
  getSnapshot = () => this.snapshot;
  subscribe = (listener: () => void) => { this.listeners.add(listener); return () => { this.listeners.delete(listener); }; };
  private publish(change: Partial<Snapshot>) {
    this.snapshot = { ...this.snapshot, ...change };
    this.listeners.forEach(listener => listener());
  }
  cancelPending = () => { ++this.lifecycle; ++this.rankingGeneration; ++this.calendarGeneration; };
  loadMonth = async (month = this.snapshot.month) => {
    const generation = ++this.calendarGeneration;
    this.publish({ month, calendar: undefined, calendarLoading: true, calendarError: '' });
    try {
      const calendar = await this.transport<SignInMonth>(`/api/points/sign-ins?month=${encodeURIComponent(month)}`);
      if (!calendar || calendar.month !== month) throw new Error('签到轨迹响应异常');
      if (generation === this.calendarGeneration) this.publish({ calendar, calendarLoading: false });
    } catch (error) {
      if (generation === this.calendarGeneration) this.publish({ calendarLoading: false, calendarError: this.message(error) });
    }
  };
  refresh = async () => {
    const generation = ++this.rankingGeneration;
    const season = utcSeason(this.now());
    this.publish({ season, loading: true, rankingError: '', ranking: [] });
    try {
      const ranking = await this.transport<RankingItem[]>(`/api/points/leaderboard?season=${season}&limit=100`);
      if (generation === this.rankingGeneration) this.publish({ ranking, loading: false });
    } catch (error) {
      if (generation === this.rankingGeneration) this.publish({ loading: false, rankingError: this.message(error) });
    }
  };
  sign = async () => {
    if (this.snapshot.signing) return;
    const lifecycle = this.lifecycle;
    this.publish({ signing: true, signError: '' });
    try {
      const signIn = await this.transport<SignIn>('/api/points/sign-ins', undefined, { method: 'POST' });
      if (lifecycle !== this.lifecycle) return;
      this.publish({ signing: false, signIn });
      // 刷新只读取实时榜，不把签到奖励乐观累加到榜单。
      await Promise.all([this.refresh(), this.loadMonth()]);
    } catch (error) {
      if (lifecycle === this.lifecycle) this.publish({ signing: false, signError: `签到结果未确认：${this.message(error)}。可重试确认，同一 UTC 日期不会重复奖励。` });
    }
  };
  private message(error: unknown) { return error instanceof Error ? error.message : '服务暂时不可用'; }
}
