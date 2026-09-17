import { Alert, Button, Select } from 'antd';
import { useNotes } from './NoteContext';

export function NoteCourseSelect() {
  const { store, draft, busy, courses, coursesError } = useNotes();
  if (!store.allowCourseSelection || draft?.mode !== 'create') return null;
  return <>
    {coursesError ? <Alert type="warning" message={coursesError} action={<Button onClick={() => void store.loadCourses()}>重试</Button>} /> : null}
    <label htmlFor="note-course">关联课程（可选）</label>
    <Select id="note-course" allowClear showSearch optionFilterProp="label" placeholder="不选择即为独立笔记"
      style={{ width: '100%' }} value={draft.courseId ?? undefined} disabled={busy || draft.locked}
      options={courses.map(course => ({ value: course.id, label: course.title }))}
      onChange={id => store.selectCourse(id ?? null)} />
  </>;
}
