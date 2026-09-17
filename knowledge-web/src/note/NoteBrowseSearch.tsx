import { Button, Input } from 'antd';
import { useNoteBrowse } from './NoteBrowseContext';

export function NoteBrowseSearch() {
  const { input, keyword, store } = useNoteBrowse();
  return <div className="note-search">
    <Input.Search aria-label="搜索公开笔记" placeholder="搜索标题或正文中的关键词" value={input} maxLength={100}
      enterButton="搜索" onChange={event => store.setInput(event.target.value)} onSearch={() => void store.search()} />
    {keyword ? <Button onClick={() => void store.clear()}>清除搜索</Button> : null}
  </div>;
}
