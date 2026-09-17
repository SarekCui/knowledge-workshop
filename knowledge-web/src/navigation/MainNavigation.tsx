import { NavLink } from 'react-router';

export function MainNavigation() {
  return <nav aria-label="主导航" className="main-nav">
    <NavLink to="/courses" className="header-nav">课程广场</NavLink>
    <NavLink to="/notes" className="header-nav">发现笔记</NavLink>
    <NavLink to="/activities" className="header-nav">拼团活动</NavLink>
  </nav>;
}
