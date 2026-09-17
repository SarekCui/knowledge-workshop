# ADR-0011：浏览器认证会话与路由

- 状态：已接受
- 日期：2026-09-14

## 决策

在第5.6轮补充浏览器认证入口`/api/iam/web/auth/login|refresh|logout`，复用现有刷新令牌轮换和族撤销服务，不新增服务/数据库。现有JSON令牌对接口兼容其他客户端。

浏览器刷新令牌仅经HttpOnly、SameSite=Strict、host-only Cookie传递，Path限制到浏览器认证入口；默认Secure，只有本机HTTP开发显式关闭。响应体仅返回访问令牌，Cache-Control=no-store；访问JWT保存在内存，不进入localStorage/sessionStorage/URL。页面启动刷新Cookie恢复会话。

Cookie接口要求`X-Web-Auth: 1`与严格Origin白名单；不能用SameSite替代CSRF防护，不能放开任意来源的凭证CORS。参考[OWASP自定义请求头与来源校验](https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html)。生产配置必须HTTPS且显式配置可信前端Origin，不将本地配置用于公网。

前端认证状态单独放入AuthContext，学习状态留在LearningContext。每页单飞刷新；401最多刷新并重放一次，其他故障不透明重试业务写请求。刷新轮换响应丢失可能需重新登录，不自动重试旧刷新令牌。退出时等待在途刷新完成，再注销Cookie，防止响应乱序恢复登录；旧业务响应不得恢复已退出会话。

使用React Router声明式路由：`/login`、`/learning`、`/learning/courses/:courseId`，未知路径404。受保护页等待登录恢复后再跳转，登录回跳仅允许本站学习路径。课程页刷新后通过真实章节API重建状态，不依赖传入组件的课程对象。部署需将非API路由回退到index.html。

## 边界

同页单飞不等于多标签页刷新协调；当前轮换重用会撤销族，多标签并发恢复可能要求重新登录，后续独立设计跨页协调。JWT注销后不即时失效，最长沿用访问令牌TTL。HttpOnly不解决XSS代发请求，必须继续维持XSS防护。

## 验收

检查Cookie属性、响应不泄露刷新令牌、来源/请求头拒绝、轮换/撤销、单飞401、退出与刷新竞态，以及浏览器刷新受保护课程页与退出后不可恢复。
