import type { RequestOptions } from '../api';
import type { CatalogCourse, CatalogDetail, CourseCategory, Page } from './types';

export type CatalogTransport = <T>(path: string, body?: unknown, options?: RequestOptions) => Promise<T>;
export type CatalogView = { kind: 'list' } | { kind: 'detail'; courseId: string };

interface Snapshot {
  status: 'idle' | 'loading' | 'success' | 'error';
  error: string;
  categories: CourseCategory[];
  courses: CatalogCourse[];
  detail?: CatalogDetail;
  keyword: string;
  appliedKeyword: string;
  categoryId: string;
  page: number;
  total: number;
}

export class CourseCatalogStore {
  private snapshot: Snapshot = {
    status: 'idle', error: '', categories: [], courses: [], keyword: '', appliedKeyword: '',
    categoryId: '', page: 1, total: 0,
  };
  private listeners = new Set<() => void>();
  private generation = 0;

  constructor(private view: CatalogView, private transport: CatalogTransport) {}

  getSnapshot = () => this.snapshot;
  subscribe = (listener: () => void) => {
    this.listeners.add(listener);
    return () => { this.listeners.delete(listener); };
  };
  cancelPending = () => { ++this.generation; };

  load = async () => {
    if (this.view.kind === 'detail') return this.loadDetail();
    const generation = ++this.generation;
    this.publish({ status: 'loading', error: '' });
    try {
      const [categories, courses] = await Promise.all([
        this.transport<CourseCategory[]>('/api/learning/catalog/categories'),
        this.fetchCourses(1),
      ]);
      if (generation === this.generation) {
        this.publish({ categories, courses: courses.items, page: courses.pageNo,
          total: courses.total, status: 'success' });
      }
    } catch (error) {
      if (generation === this.generation) this.publish({ status: 'error', error: this.message(error) });
    }
  };

  setKeyword = (keyword: string) => this.publish({ keyword });

  search = async () => {
    this.snapshot = { ...this.snapshot, appliedKeyword: this.snapshot.keyword.trim(), page: 1 };
    await this.loadCourses(1);
  };

  selectCategory = async (categoryId: string) => {
    this.snapshot = { ...this.snapshot, categoryId, page: 1 };
    await this.loadCourses(1);
  };

  changePage = async (page: number) => this.loadCourses(page);

  private loadCourses = async (page: number) => {
    const generation = ++this.generation;
    this.publish({ status: 'loading', error: '' });
    try {
      const result = await this.fetchCourses(page);
      if (generation === this.generation) {
        this.publish({ courses: result.items, page: result.pageNo, total: result.total, status: 'success' });
      }
    } catch (error) {
      if (generation === this.generation) this.publish({ status: 'error', error: this.message(error) });
    }
  };

  private loadDetail = async () => {
    if (this.view.kind !== 'detail') return;
    const generation = ++this.generation;
    this.publish({ status: 'loading', error: '' });
    try {
      const detail = await this.transport<CatalogDetail>(
        `/api/learning/catalog/courses/${encodeURIComponent(this.view.courseId)}`,
      );
      if (generation === this.generation) this.publish({ detail, status: 'success' });
    } catch (error) {
      if (generation === this.generation) this.publish({ status: 'error', error: this.message(error) });
    }
  };

  private fetchCourses(page: number) {
    const params = new URLSearchParams({ pageNo: String(page), pageSize: '12' });
    if (this.snapshot.categoryId) params.set('categoryId', this.snapshot.categoryId);
    if (this.snapshot.appliedKeyword) params.set('keyword', this.snapshot.appliedKeyword);
    return this.transport<Page<CatalogCourse>>(`/api/learning/catalog/courses?${params}`);
  }

  private publish(change: Partial<Snapshot>) {
    this.snapshot = { ...this.snapshot, ...change };
    this.listeners.forEach(listener => listener());
  }

  private message(error: unknown) {
    return error instanceof Error ? error.message : '课程加载失败';
  }
}
