# Note分享第一批后端验收

- 日期：2026-09-14；设计见docs/14和ADR-0012。
- 范围：可选课程、DRAFT/PRIVATE/PUBLIC、状态切换、本人分页、公开分页/详情和标题正文搜索。无首页前端、图片上传、互动或热门排序。

## 实际结果

- 学习服务16个单元测试通过；其依赖common4个、security2个单元测试通过。
- LearningAcceptanceIT全部15个隔离集成测试通过，无跳过；新增2项覆盖无课程幂等创建、课程关联无权益创作、状态默认PRIVATE、公开搜索/过滤、跨用户权限、版本冲突、撤回/删除不再公开、空正文发布拒绝、页大小约束。既有课程、权益、视频进度与Note回归通过。
- 前端13个测试文件、59个测试与构建通过；本轮未改前端。大包警告仍存在。
- git diff --check通过。

命令（项目根目录，Java17，现有Maven缓存与隔离Docker环境）：

```sh
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home mvn -o -Dmaven.repo.local=.m2/repository -pl knowledge-learning -am test test-compile failsafe:integration-test failsafe:verify -Dit.test=LearningAcceptanceIT -Dfailsafe.failIfNoSpecifiedTests=false
cd knowledge-web
npm test -- --run
npm run build
```

## 运行与遗留

未替换或重启当前运行的learning JAR，未对用户本地库执行V4；迁移已在隔离空库执行通过。现有页面仍为旧私人Note界面，不能宣称新分享前端已交付或浏览器已验收。

下批接入前需安全发布learning增量并执行Flyway迁移；现有私人笔记不自动公开。状态切换重试须刷新版本确认，旧version返回409；无互联网内容审核治理，不宣称生产开放。

此前旧版Note浏览器创建/搜索/编辑/重命名/删除已实际通过，仅删除本轮唯一验收笔记。旧版购买流程仅到测试账号参团生成订单948b10cb-1150-4df8-8a37-26d41a629874，支付未确认且已取消浏览器弹窗；未宣称成团/权益浏览器验收完成。

第5.6整体仍进行中，本记录不代替全仓库后端回归。
