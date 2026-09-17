# 知识工坊学习端

React + TypeScript + Ant Design，Vite 连接本地网关，所有页面使用实际业务接口，无 Mock 数据。

## 启动

要求 Node.js 20+ 与 npm。先按仓库 README 启动 IAM、学习服务和网关，三者必须使用同一个 JWT_SECRET。

```bash
cd knowledge-web
npm ci --registry=https://registry.npmjs.org
npm run dev
```

访问 http://127.0.0.1:5173。开发代理将 `/api` 转发到网关 `127.0.0.1:8080`；生产部署需要配置同源反向代理，不能直接使用开发服务器。

## 结构

- `learning/LearningContext.tsx`：限定在学习页面的共享状态、接口操作与 `useLearning`。
- `LoginPanel`、`CourseList`、`ChapterList`、`LearningWorkspace`：展示与交互组件，不层层传递状态和方法。
- `Player`：播放器展示，通过 `usePlayback` 获取能力。
- `usePlayback`：媒体事件、断点恢复、区间收集与生命周期清理。
- `progress`：纯区间判断、有界单飞重试队列。
- `api`：Bearer 请求头、响应解包、超时与错误转换。

## 检查与边界

```bash
npm test
npm run build
npm audit --registry=https://registry.npmjs.org
```

- JWT 仅存在内存，不写入 localStorage；重载需重新登录，刷新令牌轮换 UI 待接入。
- 心跳15秒、失败重试5秒；重试保持完整原事件。队列最多100条，满时暂停播放；409停止旧会话上报。
- 暂停、拖动、结束和切到后台触发上报；实际连续播放区间才计算完成度。
- 队列尚未持久化，卸载只尝试最后提交，刷新/关闭页面可能丢失未确认事件；不宣称离线可靠投递完成。
- 原生播放器不保证所有浏览器支持 HLS；暂不引入测试视频、占位课程或演示业务页面。可播放视频与有效权益由真实后台数据提供。
- Ant Design 的 `use client` 构建提示与首屏包大小警告尚存，属于后续打包优化项，不能视为运行错误或已完成性能优化。
