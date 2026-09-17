# 学习端登录与页面路由

## 使用

前端地址http://127.0.0.1:5173，经Vite同源代理请求网关8080。账号`demo` / `Knowledge@123`仅用于已有本机测试环境，真实课程数据初始化见`docs/11-local-test-video.md`。

- `/login`：登录，错误可见；游客在公开页触发点赞、收藏、评论或获取课程后，登录回跳原页。
- `/courses`、`/courses/{courseId}`：公开课程广场与课程详情，游客可直接浏览。
- `/notes`、`/notes/course/{courseId}`、`/notes/{noteId}`：公开笔记列表与独立详情页，游客可阅读正文和评论。
- `/learning`：本人课程分页列表，需要登录。
- `/learning/courses/{courseId}`：真实章节与播放器；刷新页面后恢复登录并重新查询章节，点击打开视频恢复后端断点。播放器实例不持久化，不在页面刷新时自动播放。
- 头像下拉菜单承载“我的课程、我的笔记、我的点赞、我的收藏、我的订单、签到积分、个人资料”等个人入口；公共主导航只保留发现型内容。
- 未知路由显示404，不自动伪装成首页。

## 本地配置

IAM默认`WEB_AUTH_COOKIE_SECURE=true`。本机HTTP开发启动IAM时显式设置：

```bash
export WEB_AUTH_COOKIE_SECURE=false
export WEB_AUTH_ALLOWED_ORIGINS=http://127.0.0.1:5173,http://localhost:5173
```

不要用于公网。生产必须HTTPS、Secure=true、显式可信前端Origin，并将Web与`/api`反向代理到同一Origin；非API前端路径需回退index.html，API错误不能回退HTML。不要设置刷新Cookie Domain或开放任意凭证CORS。

## 认证契约

新增POST `/api/iam/web/auth/login`接收LoginDTO；refresh/logout使用HttpOnly `kw_refresh` Cookie，不接收JS刷新令牌。三者必须携带`X-Web-Auth: 1`，Origin必须匹配IAM白名单。服务返回no-store，登录/刷新仅返回访问令牌、类型与有效期；Cookie每次刷新轮换，注销撤销令牌族并清Cookie。原`/api/iam/auth/*`契约不变。

访问JWT只在内存，经Authorization Bearer调用业务接口；页面启动通过Cookie换新JWT。AuthContext只管理认证，LearningContext只管理学习；401同页单飞刷新后最多重放一次，保留原请求体和事件UUID。503、超时不透明重试业务写请求；播放器自身事件队列仍按原事件重试。

刷新Cookie不是业务授权，服务仍独立验JWT和课程权益。初始无Cookie正常显示登录；刷新拒绝显示过期，依赖故障提供恢复重试。退出等待在途刷新后再发注销，防止新Cookie响应覆盖注销；失败显示“退出请求未确认”并提供重试，不宣称服务端已撤销。

## 已知边界

- 当前只做同页单飞，多个标签页同时轮换同一个Cookie仍可能触发族撤销，需要重新登录。
- 刷新轮换超时结果可能未知，不能反复重试旧凭证保证恢复。
- 退出不立即撤销已签发JWT，最长有效至现有30分钟TTL；不宣称即时全设备下线。
- HttpOnly不能防止XSS代发请求，Cookie Path不是完整安全隔离边界。
- 关闭页面最终进度、持久化离线队列和Note UI不属于本批。

策略依据与进一步验收见ADR-0011及`docs/acceptance/round-5.6-web-auth-routing.md`。
