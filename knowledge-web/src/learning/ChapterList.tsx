import {Button, Card, Empty, Skeleton} from 'antd';
import {useLearning} from './LearningContext';
import {formatDuration} from './formatDuration';

export function ChapterList() {
    const {chapters, busy, playing, openVideo} = useLearning();
    return <Card className="chapter-panel" title="课程目录" extra={<span className="muted">{chapters.length} 节</span>}>
        {busy && !chapters.length ? <Skeleton active/> : !chapters.length ?
            <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无可学习章节"/>
            : <ol className="chapter-list">{chapters.map((chapter, index) => {
                const selected = playing?.chapter.id === chapter.id;
                return <li key={chapter.id}><Button className={`chapter-button${selected ? ' is-active' : ''}`}
                                                    disabled={busy}
                                                    aria-current={selected ? 'true' : undefined}
                                                    onClick={() => void openVideo(chapter)}>
                    <span className="chapter-number">{String(index + 1).padStart(2, '0')}</span>
                    <span className="chapter-info"><span>{chapter.title}</span><span
                        className="chapter-duration">{formatDuration(chapter.videoDurationMs)}{selected ? ' · 当前章节' : ''}</span></span>
                </Button></li>;
            })}</ol>}
    </Card>;
}
