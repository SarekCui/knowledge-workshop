import { Alert, Button, Card, Empty, List, Pagination, Popconfirm, Skeleton, Space, Tag, Typography } from 'antd';
import { Link } from 'react-router';
import { useNotes } from './NoteContext';
import { NoteEditor } from './NoteEditor';
import { NoteSearch } from './NoteSearch';
import { markdownExcerpt } from './markdown';

export function NotePanel() {
  const { notes, status, busy, error, notice, draft, keyword, store, page, total } = useNotes();
  return <Card className="note-panel" title="Note" extra={<Space>
    <Button disabled={busy || !!draft} onClick={() => void store.load()}>刷新</Button>
    <Button type="primary" disabled={busy || !!draft || status === 'loading'} onClick={store.create}>新建 Note</Button>
  </Space>}>
    <Typography.Paragraph type="secondary">{store.paginated ? '用 Markdown 沉淀技术实践、代码片段和问题解决过程。' : '记录这门课程的技术要点。当前显示最近100篇。'}新建默认草稿，保存后可主动发布；编辑中的内容不会自动保存。</Typography.Paragraph>
    <NoteSearch />
    {keyword ? <Typography.Paragraph type="secondary">{store.paginated ? '标题/正文搜索' : '名称搜索'}：{keyword}{store.paginated ? '' : ' · 显示最近100篇匹配结果'}</Typography.Paragraph> : null}
    {error && !draft ? <Alert type="error" message={error} showIcon /> : null}
    {notice ? <Alert type="success" message={notice} showIcon /> : null}
    {status === 'loading' ? <div role="status"><Skeleton active />正在加载 Note</div>
      : status === 'error' ? <Button disabled={busy || !!draft} onClick={() => void store.load()}>重新加载 Note</Button>
      : status === 'success' ? <List dataSource={notes} locale={{ emptyText: <Empty description={keyword ? '没有找到匹配名称的 Note' : '还没有 Note，记录第一个想法吧'} /> }}
        renderItem={note => <List.Item className="note-row"><div className="note-preview">
          <Typography.Title level={4}>{note.title}</Typography.Title>
          <Tag>{note.status === 'PUBLIC' ? '公开' : note.status === 'DRAFT' ? '草稿' : '私人'}</Tag>
          <Tag>{note.courseId ? '关联课程' : '独立笔记'}</Tag>
          {note.tags?.map(item => <Tag key={item}>#{item}</Tag>)}
          <p>{markdownExcerpt(note.content) || '暂无正文'}</p>
        </div><Space wrap>
          {note.status === 'PUBLIC' ? <>
            <Link to={`/notes/${encodeURIComponent(note.id)}`}>查看公开详情</Link>
            <Popconfirm title="撤回为草稿？" description="撤回后其他用户不可见，之后可以编辑。" onConfirm={() => store.changeStatus(note.id, 'DRAFT')}>
              <Button disabled={busy || !!draft}>撤回 / 编辑</Button>
            </Popconfirm>
          </> : <>
            <Button disabled={busy || !!draft} onClick={() => void store.open(note.id)}>查看 / 编辑</Button>
            <Button disabled={busy || !!draft} onClick={() => void store.open(note.id, 'rename')}>重命名</Button>
            <Popconfirm title="公开发布这篇 Note？" description="发布后其他登录用户可浏览并搜索到正文。" onConfirm={() => store.changeStatus(note.id, 'PUBLIC')}>
              <Button disabled={busy || !!draft}>发布</Button>
            </Popconfirm>
            {note.status === 'DRAFT' ? <Button disabled={busy || !!draft} onClick={() => void store.changeStatus(note.id, 'PRIVATE')}>设为私人</Button> : null}
          </>}
          <Popconfirm title="删除这篇 Note？" description="删除后将不再显示。" disabled={busy || !!draft} onConfirm={() => store.remove(note.id)}>
            <Button danger disabled={busy || !!draft}>删除</Button>
          </Popconfirm>
        </Space></List.Item>} /> : null}
    {store.paginated && total > 20 ? <Pagination current={page} total={total} pageSize={20} showSizeChanger={false}
      disabled={busy || !!draft || status === 'loading'} onChange={next => void store.load(keyword, next)} /> : null}
    <NoteEditor />
  </Card>;
}
