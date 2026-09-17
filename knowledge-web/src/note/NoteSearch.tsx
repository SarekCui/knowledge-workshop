import { Button, Input } from 'antd';
import { useNotes } from './NoteContext';

export function NoteSearch() {
  const { searchInput, keyword, busy, draft, store } = useNotes();
  const disabled = busy || !!draft;
  return <div className="note-search">
    <Input.Search aria-label="按 Note 名称搜索" placeholder="搜索 Note 名称" value={searchInput} maxLength={100}
      disabled={disabled} enterButton="搜索" onChange={event => store.setSearchInput(event.target.value)}
      onSearch={() => void store.search()} />
    {keyword ? <Button disabled={disabled} onClick={() => void store.clearSearch()}>清除搜索</Button> : null}
  </div>;
}
