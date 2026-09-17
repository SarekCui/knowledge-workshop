import { createContext, useCallback, useContext, useEffect, useRef, useState, type ReactNode } from 'react';
import { useNavigate } from 'react-router';
import { type Chapter, type Course, type Session } from '../api';
import { useAuth } from '../auth/AuthContext';

function useLearningState() {
  const { apiRequest } = useAuth();
  const navigate = useNavigate();
  const [courses, setCourses] = useState<Course[]>([]);
  const [chapters, setChapters] = useState<Chapter[]>([]);
  const [playing, setPlaying] = useState<{ chapter: Chapter; session: Session }>();
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [page, setPage] = useState(1);
  const [total, setTotal] = useState(0);
  const [courseStatus, setCourseStatus] = useState<'idle' | 'loading' | 'success' | 'error'>('idle');
  const [courseError, setCourseError] = useState('');
  const busyRef = useRef(false);
  const alive = useRef(true);
  useEffect(() => { alive.current = true; return () => { alive.current = false; }; }, []);
  const task = useCallback(async (action: () => Promise<void>) => {
    if (busyRef.current) return;
    busyRef.current = true; setBusy(true); setError('');
    try { await action(); }
    catch (failure) { if (alive.current) setError(failure instanceof Error ? failure.message : '请求失败'); }
    finally { busyRef.current = false; if (alive.current) setBusy(false); }
  }, []);
  const refreshCourses = useCallback((pageNo = 1) => task(async () => {
    setCourseStatus('loading'); setCourseError('');
    try {
      const data = await apiRequest<{ items: Course[]; total: number }>(`/api/learning/my-courses?pageNo=${pageNo}&pageSize=20`);
      if (!alive.current) return;
      setCourses(data.items); setTotal(data.total); setPage(pageNo); setCourseStatus('success');
    } catch (failure) {
      if (!alive.current) return;
      setCourseError(failure instanceof Error ? failure.message : '请求失败'); setCourseStatus('error');
    }
  }), [apiRequest, task]);
  const selectCourse = (courseId: string) => navigate(`/learning/courses/${encodeURIComponent(courseId)}`);
  const loadCourse = useCallback((courseId: string) => {
    let active = true;
    setChapters([]); setPlaying(undefined); setError(''); setBusy(true);
    void apiRequest<Chapter[]>(`/api/learning/courses/${encodeURIComponent(courseId)}/chapters`)
      .then(data => { if (active && alive.current) setChapters(data); })
      .catch(failure => { if (active && alive.current) setError(failure instanceof Error ? failure.message : '章节加载失败'); })
      .finally(() => { if (active && alive.current) setBusy(false); });
    return () => { active = false; };
  }, [apiRequest]);
  const openVideo = (chapter: Chapter) => task(async () => {
    const session = await apiRequest<Session>(`/api/learning/videos/${encodeURIComponent(chapter.videoId)}/sessions`, {});
    if (alive.current) setPlaying({ chapter, session });
  });
  return { courses, chapters, playing, error, busy, page, total, courseStatus, courseError,
    refreshCourses, selectCourse, openVideo, loadCourse, apiRequest };
}
const LearningContext = createContext<ReturnType<typeof useLearningState> | null>(null);
export function LearningProvider({ children }: { children: ReactNode }) {
  const state = useLearningState();
  return <LearningContext.Provider value={state}>{children}</LearningContext.Provider>;
}
export function useLearning() {
  const state = useContext(LearningContext);
  if (!state) throw new Error('useLearning 必须位于 LearningProvider 内');
  return state;
}
