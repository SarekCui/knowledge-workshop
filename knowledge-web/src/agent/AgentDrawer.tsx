import { Alert, Button, Drawer, Empty, FloatButton, Input, Popconfirm, Spin, Typography } from 'antd';
import { ArrowLeftOutlined, DeleteOutlined, HistoryOutlined, MessageOutlined, PlusOutlined, RobotOutlined, SendOutlined } from '@ant-design/icons';
import { useEffect, useMemo, useRef, useState, useSyncExternalStore } from 'react';
import type { MouseEvent as ReactMouseEvent } from 'react';
import { useLocation, useNavigate } from 'react-router';
import { useAuth } from '../auth/AuthContext';
import { MarkdownDocument } from '../note/MarkdownDocument';
import { AgentChatStore } from './AgentChatStore';
import { createAgentChatTransport } from './agentSse';

function formatElapsed(seconds: number) {
  const mm = String(Math.floor(seconds / 60)).padStart(2, '0');
  const ss = String(seconds % 60).padStart(2, '0');
  return `${mm}:${ss}`;
}

function formatConversationTime(iso: string) {
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return '';
  const now = new Date();
  const sameDay = date.toDateString() === now.toDateString();
  const yesterday = new Date(now); yesterday.setDate(now.getDate() - 1);
  const isYesterday = date.toDateString() === yesterday.toDateString();
  const hh = String(date.getHours()).padStart(2, '0');
  const mm = String(date.getMinutes()).padStart(2, '0');
  if (sameDay) return `${hh}:${mm}`;
  if (isYesterday) return `昨天 ${hh}:${mm}`;
  return `${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

const DEFAULT_PANEL_WIDTH = 440;
const MIN_PANEL_WIDTH = 360;
const MAX_PANEL_WIDTH = 720;
const PANEL_WIDTH_STORAGE_KEY = 'agent-panel-width';

function loadPanelWidth() {
  if (typeof window === 'undefined') return DEFAULT_PANEL_WIDTH;
  const saved = Number(window.localStorage.getItem(PANEL_WIDTH_STORAGE_KEY));
  return Number.isFinite(saved) && saved >= MIN_PANEL_WIDTH && saved <= MAX_PANEL_WIDTH
    ? saved : DEFAULT_PANEL_WIDTH;
}

export function AgentDrawer() {
  const { status: authStatus, apiRequest, agentStreamRequest } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const [input, setInput] = useState('');
  const [isMobile, setIsMobile] = useState(() =>
    typeof window !== 'undefined' ? window.matchMedia('(max-width: 560px)').matches : false);
  const [panelWidth, setPanelWidth] = useState(loadPanelWidth);
  const [resizing, setResizing] = useState(false);
  const [, setTick] = useState(0);
  const bottomRef = useRef<HTMLDivElement>(null);
  const panelWidthRef = useRef(panelWidth);
  panelWidthRef.current = panelWidth;
  const store = useMemo(() => new AgentChatStore(createAgentChatTransport(apiRequest, agentStreamRequest)),
    [apiRequest, agentStreamRequest]);
  const snapshot = useSyncExternalStore(store.subscribe, store.getSnapshot, store.getSnapshot);
  const busy = snapshot.status === 'creating' || snapshot.status === 'streaming';
  const authenticated = authStatus === 'authenticated';

  useEffect(() => {
    const media = window.matchMedia('(max-width: 560px)');
    const onChange = (event: MediaQueryListEvent) => setIsMobile(event.matches);
    media.addEventListener('change', onChange);
    return () => media.removeEventListener('change', onChange);
  }, []);

  // 桌面端打开时，主内容区让出面板宽度，使小智成为常驻侧边栏而非模态遮罩。
  useEffect(() => {
    const docked = open && !isMobile;
    document.body.classList.toggle('agent-panel-open', docked);
    return () => document.body.classList.remove('agent-panel-open');
  }, [open, isMobile]);

  // 面板宽度通过 CSS 变量同时驱动 Drawer 宽度与主内容区避让距离。
  useEffect(() => {
    document.body.style.setProperty('--agent-panel-width', `${panelWidth}px`);
  }, [panelWidth]);

  // 拖拽左边缘调宽：全屏 overlay 接管鼠标事件，避免视频/iframe 吞掉 mousemove。
  useEffect(() => {
    if (!resizing) return;
    document.body.classList.add('agent-panel-resizing');
    const onMouseMove = (event: MouseEvent) => {
      const maxWidth = Math.min(MAX_PANEL_WIDTH, Math.floor(window.innerWidth * 0.85));
      const width = Math.min(maxWidth, Math.max(MIN_PANEL_WIDTH, window.innerWidth - event.clientX));
      setPanelWidth(width);
    };
    const onMouseUp = () => {
      setResizing(false);
      try { window.localStorage.setItem(PANEL_WIDTH_STORAGE_KEY, String(panelWidthRef.current)); }
      catch { /* 隐私模式等场景下忽略持久化失败 */ }
    };
    document.addEventListener('mousemove', onMouseMove);
    document.addEventListener('mouseup', onMouseUp);
    return () => {
      document.body.classList.remove('agent-panel-resizing');
      document.removeEventListener('mousemove', onMouseMove);
      document.removeEventListener('mouseup', onMouseUp);
    };
  }, [resizing]);

  const startResize = (event: ReactMouseEvent<HTMLDivElement>) => {
    if (isMobile) return;
    event.preventDefault();
    setResizing(true);
  };

  // 问答进行中每秒刷新一次，驱动气泡上方的思考计时跳动。
  useEffect(() => {
    if (!busy) return;
    const timer = window.setInterval(() => setTick(value => value + 1), 1000);
    return () => window.clearInterval(timer);
  }, [busy]);

  useEffect(() => { bottomRef.current?.scrollIntoView({ block: 'end' }); }, [snapshot.messages, snapshot.status]);

  const submit = () => {
    if (!input.trim() || busy) return;
    const question = input;
    setInput('');
    void store.ask(question, `当前页面：${location.pathname}`);
  };
  const requestLogin = () => {
    setOpen(false);
    navigate('/login', { state: { from: location.pathname } });
  };

  return <>
    <FloatButton className="agent-launcher" type="primary" icon={<RobotOutlined />} tooltip="问小智"
      aria-label="打开小智学习助手" onClick={() => setOpen(true)} />
    {open && !isMobile ? <div
      className="agent-resize-handle"
      role="separator"
      aria-orientation="vertical"
      aria-label="拖动调整小智窗口宽度，双击恢复默认宽度"
      aria-valuenow={panelWidth}
      aria-valuemin={MIN_PANEL_WIDTH}
      aria-valuemax={MAX_PANEL_WIDTH}
      onMouseDown={startResize}
      onDoubleClick={() => {
        setPanelWidth(DEFAULT_PANEL_WIDTH);
        try { window.localStorage.setItem(PANEL_WIDTH_STORAGE_KEY, String(DEFAULT_PANEL_WIDTH)); } catch { /* ignore */ }
      }} /> : null}
    {resizing ? <div className="agent-resize-overlay" aria-hidden="true" /> : null}
    <Drawer className="agent-drawer" title={<span className="agent-drawer-title"><RobotOutlined /> 小智 <small>AI 学习助手</small></span>}
      placement="right" width={isMobile ? DEFAULT_PANEL_WIDTH : panelWidth} open={open} onClose={() => setOpen(false)} destroyOnClose={false}
      mask={isMobile} maskClosable={isMobile}
      extra={<div className="agent-drawer-extra">
        {snapshot.view === 'history'
          ? <Button type="text" icon={<ArrowLeftOutlined />} onClick={store.closeHistory}>返回</Button>
          : <Button type="text" icon={<HistoryOutlined />} onClick={store.openHistory}>历史</Button>}
        <Button type="text" icon={<PlusOutlined />} disabled={busy || !snapshot.messages.length} onClick={store.clear}>新对话</Button>
      </div>}>
      {!authenticated ? <div className="agent-login-state">
        <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="登录后即可向小智提问" />
        <Button type="primary" onClick={requestLogin}>去登录</Button>
      </div> : <div className="agent-chat-layout">
        {snapshot.view === 'history' ? <div className="agent-history-list" role="listbox" aria-label="历史会话">
          {snapshot.loadingHistory ? <div className="agent-history-loading"><Spin size="small" /> 正在加载历史会话…</div>
            : !snapshot.conversations.length ? <div className="agent-empty-state">
              <HistoryOutlined aria-hidden="true" />
              <Typography.Title level={5}>暂无历史会话</Typography.Title>
              <p>和小智的第一次对话会自动保存在这里。</p>
            </div>
            : snapshot.conversations.map(conv => <div key={conv.id} className="agent-history-item"
              role="option" aria-selected={conv.id === snapshot.conversationId}
              onClick={() => void store.selectConversation(conv.id)}>
              <span className="agent-history-text">
                <span className="agent-history-title">{conv.title}</span>
                <time dateTime={conv.updatedAt}>{formatConversationTime(conv.updatedAt)}</time>
              </span>
              <Popconfirm title="删除这条会话？" description="对话记录将被彻底删除，无法恢复。"
                okText="删除" cancelText="取消" okButtonProps={{ danger: true }}
                onConfirm={event => { event?.stopPropagation(); void store.deleteConversation(conv.id); }}
                onCancel={event => event?.stopPropagation()}>
                <Button type="text" size="small" className="agent-history-delete" aria-label="删除会话"
                  icon={<DeleteOutlined />} onClick={event => event.stopPropagation()} />
              </Popconfirm>
            </div>)}
        </div> : <>
        <div className="agent-message-list" role="log" aria-live="polite" aria-label="小智问答记录">
          {!snapshot.messages.length ? <div className="agent-empty-state">
            <RobotOutlined aria-hidden="true" />
            <Typography.Title level={4}>你好，我是小智</Typography.Title>
            <p>可以问我课程知识点、学习方法，或当前页面相关的问题。</p>
          </div> : snapshot.messages.map(message => {
            const liveElapsed = message.role === 'assistant' && message.status === 'streaming' && snapshot.runStartedAt
              ? formatElapsed(Math.floor((Date.now() - snapshot.runStartedAt) / 1000)) : null;
            const finalElapsed = message.role === 'assistant' && message.elapsedMs != null
              ? formatElapsed(Math.round(message.elapsedMs / 1000)) : null;
            return <article key={message.id}
            className={`agent-message agent-message-${message.role}`}>
            <span className="agent-message-label">{message.role === 'user' ? ''
              : <span className="agent-message-identity">
                  <RobotOutlined className="agent-message-avatar" aria-hidden="true" />
                  <span>小智</span>
                  {liveElapsed ? <span className="agent-elapsed agent-elapsed-live" aria-live="off">思考中 {liveElapsed}</span>
                    : finalElapsed ? <span className="agent-elapsed">已思考 {finalElapsed}</span> : null}
                </span>}</span>
            <div className="agent-message-content">
              {message.role === 'assistant'
                ? message.content ? <MarkdownDocument content={message.content} />
                  : message.status === 'streaming'
                    ? <span className="agent-thinking"><Spin size="small" /> 小智正在思考</span>
                    : <span>小智暂时没有完成回答。</span>
                : message.content}
            </div>
          </article>;})}
          <div ref={bottomRef} />
        </div>
        {snapshot.error ? <Alert type="error" showIcon message={snapshot.error} closable
          onClose={() => undefined} /> : null}
        <div className="agent-composer">
          <Input.TextArea value={input} autoSize={{ minRows: 3, maxRows: 6 }} maxLength={6000}
            disabled={busy} placeholder="输入问题，Enter 发送，Shift + Enter 换行"
            aria-label="向小智提问" onChange={event => setInput(event.target.value)}
            onPressEnter={event => { if (!event.shiftKey) { event.preventDefault(); submit(); } }} />
          <div className="agent-composer-footer">
            <span>{input.length} / 6000</span>
            <Button type="primary" icon={<SendOutlined />} disabled={!input.trim()} loading={busy} onClick={submit}>发送</Button>
          </div>
        </div>
        <p className="agent-disclaimer"><MessageOutlined aria-hidden="true" /> 回答仅供学习参考；当前版本暂不提供来源引用。</p>
        </>}
      </div>}
    </Drawer>
  </>;
}
