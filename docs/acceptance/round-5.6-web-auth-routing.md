# 第5.6轮登录与路由切片验收

- 日期：2026-09-14；当前工作区未提交，不提供新commit。
- 环境：macOS、Java17、Node20.17.0、React18.3.1、ReactRouter7.18.3、Vite6.4.3、AntDesign5.27.6、MySQL8.4。
- 范围：登录恢复、访问令牌刷新、注销及三个独立页面；不是第5.6轮整体完成。

## 自动化

```bash
cd knowledge-web
npm test
npm run build
npm audit --registry=https://registry.npmjs.org
```

前端19项测试通过，TypeScript严格检查与生产构建通过，依赖审计0已知漏洞。新增6项认证测试覆盖启动Cookie恢复、并发401单飞、原请求重放、503不重试、刷新拒绝/依赖故障区别、刷新后仍401停止、退出与刷新竞态。

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home mvn -Dmaven.repo.local=.m2/repository -pl knowledge-iam,knowledge-gateway -am verify -q
```

最终执行退出0：IAM集成5项、网关容错集成3项，失败/错误/跳过均0，相关模块单元测试通过；不将此结果称为全仓库本轮回归。IAM新增浏览器Cookie轮换/注销和来源/专用Header拒绝场景；断言HttpOnly、Secure、SameSite=Strict、限定Path、no-store、JSON不返回刷新令牌，注销后的Cookie刷新401。

## 浏览器

- 使用真实5173页面、网关8080、IAM8083和学习8084，不替换业务API。
- 未登录可直达课程广场、课程详情、公开笔记列表和笔记详情；直达`/learning`或学习播放页仍跳转登录，登录后回到原受保护页面。
- 登录JSON不包含refreshToken，document.cookie不可见刷新凭证，localStorage/sessionStorage均无令牌数据。
- 刷新课程页恢复认证并重新查询章节；打开视频恢复已落库12.345秒，等待seeked后位置一致。
- 退出后刷新仍在登录页面；同页登录状态与服务端刷新会话不再恢复。
- 最终配置下新会话复验登录、课程页直接访问及刷新、退出后刷新、未知路由404；`errors`最终为空。
- 添加路由依赖期间发现旧Vite预构建混用React Hook错误，补resolve.dedupe与固定预构建项，使用`npm run dev -- --force`重启。重新开新浏览器会话复验，登录表单可见，无Vite错误层，errors为空；历史错误保留为已观察问题，不将旧会话日志伪装成无错误。
- React最佳实践检查促使认证与学习Context分离，课程异步请求编排留在业务Hook，页面不接收业务状态/方法props；401刷新不因访问令牌变更重建播放器或重置序号。

## 遗留

包大小及AntDesign指令提示仍存在；不宣称性能验收完成。多标签页刷新协调、JWT即时撤销、持久化离线队列、关闭/注销时最终进度可靠投递、Note UI仍后置。生产HTTPS、可信Origin与同源反向代理尚未部署验收。本机显式WEB_AUTH_COOKIE_SECURE=false，不得用于公网。

使用说明：`docs/12-web-auth-and-routing.md`；设计：ADR-0011。

## 2026-09-17 公开浏览与导航增量

- 游客真实浏览课程广场、课程详情、公开笔记列表、笔记详情与评论，页面不重定向登录；点赞等写操作才进入登录页，并在登录后返回原笔记。
- 顶部公共导航保留课程广场、发现笔记和拼团活动；订单、本人课程、本人笔记、点赞、收藏、积分及个人资料统一放入登录账号下拉菜单。
- 网关及资源服务只匿名放行明确列出的公开GET接口，写接口和本人数据接口仍要求JWT；公开响应中的`liked`、`favorited`对游客为`false`。
