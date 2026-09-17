export interface CourseCategory {
  id: string;
  name: string;
}

export interface CatalogCourse {
  id: string;
  title: string;
  summary: string;
  coverUrl: string | null;
  priceCents: number;
  categoryId: string;
  categoryName: string;
  chapterCount: number;
  totalDurationMs: number;
  entitled: boolean;
  updatedAt: string;
}

export interface ChapterSummary {
  id: string;
  title: string;
  sortOrder: number;
  videoDurationMs: number;
}

export interface CatalogDetail {
  course: CatalogCourse;
  chapters: ChapterSummary[];
}

export interface Page<T> {
  items: T[];
  pageNo: number;
  pageSize: number;
  total: number;
}
