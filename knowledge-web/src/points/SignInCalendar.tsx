import { Alert, Button, Card, Skeleton, Space } from 'antd';
import { usePoints } from './PointsContext';
import { monthCells, moveMonth } from './calendar';

const weekdays = ['一', '二', '三', '四', '五', '六', '日'];
export function SignInCalendar() {
  const { month, calendar, calendarLoading, calendarError, loadMonth } = usePoints();
  const signed = new Set(calendar?.signedDates ?? []);
  return <Card title="签到轨迹">
    <div className="signin-calendar-toolbar">
      <Space><Button aria-label="上个月" disabled={month === '0001-01'} onClick={() => void loadMonth(moveMonth(month, -1))}>‹</Button>
        <strong>{month.replace('-', ' 年 ')} 月</strong>
        <Button aria-label="下个月" disabled={month === '9999-12' || (!!calendar && month >= calendar.today.slice(0, 7))}
          onClick={() => void loadMonth(moveMonth(month, 1))}>›</Button></Space>
      <Button onClick={() => void loadMonth(calendar?.today.slice(0, 7) ?? new Date().toISOString().slice(0, 7))}>本月</Button>
    </div>
    <p>按 UTC 日期记录 · {calendar ? `本月已签到 ${signed.size} 天` : '正在确认签到轨迹'}</p>
    {calendarError ? <Alert type="error" showIcon message="签到轨迹加载失败" description={calendarError}
      action={<Button onClick={() => void loadMonth()}>重试</Button>} /> : calendarLoading || !calendar ? <Skeleton active /> :
      <div className="signin-calendar-grid" aria-label={`${month} 签到日历`}>
        {weekdays.map(day => <div className="signin-weekday" key={day}>周{day}</div>)}
        {monthCells(month).map((date, index) => date ? <div key={date}
          aria-label={`${date} ${signed.has(date) ? '已签到' : date > calendar.today ? '未到日期' : '未签到'}`}
          aria-current={date === calendar.today ? 'date' : undefined}
          className={`signin-day${signed.has(date) ? ' is-signed' : ''}${date === calendar.today ? ' is-today' : ''}${date > calendar.today ? ' is-future' : ''}`}>
          <span>{Number(date.slice(-2))}</span>
          <small>{signed.has(date) ? '✓ 已签' : date === calendar.today ? '今天' : date > calendar.today ? '—' : '未签'}</small>
        </div> : <div key={`blank-${index}`} aria-hidden="true" />)}
      </div>}
  </Card>;
}
