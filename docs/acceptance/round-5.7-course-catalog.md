# 第 5.7 轮：课程目录与安全学习入口验收

## 自动化结果

- `knowledge-learning` 及依赖模块：26 个单元测试通过，无失败、无跳过。
- `LearningAcceptanceIT`：19 个真实 MySQL、Redis、RabbitMQ 容器场景通过，无失败、无跳过。
- 课程目录新增场景覆盖分类/关键词筛选、章节聚合、用户权益标记、安全目录不泄露 `videoUrl`，以及无权益用户不能读取完整播放章节。
- 前端：23 个测试文件、88 个测试通过；TypeScript 检查和 Vite 生产构建通过。

## 执行命令

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home \
  mvn -Dmaven.repo.local=.m2/repository -pl knowledge-learning -am verify -Pacceptance

cd knowledge-web
npm test
npm run build
```

## 结论

课程发现和已购学习入口已形成真实数据闭环。分页权益判断在单次查询内完成，没有逐课程查询；详情目录与播放目录分离，服务端权益校验是最终授权边界。当前结果是功能正确性验收，不代表生产容量或搜索质量压测。
