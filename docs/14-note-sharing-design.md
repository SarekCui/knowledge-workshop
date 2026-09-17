# Note知识分享设计

设计决策见ADR-0012。第一批为后端契约增量；第二批接入文字笔记信息流、详情、本人创作、课程关联、发布和搜索；第三批建设点赞、收藏、评论与真实热门排序。图文上传、举报和审核仍后置。

## 状态与迁移

- 新创建为DRAFT；既有记录通过V4迁移默认PRIVATE，不自动公开。
- PRIVATE与DRAFT均仅本人管理可见。PUBLIC可被其他登录用户在公开接口浏览。
- 发布要求正文非空。PUBLIC修改标题或正文前先显式切回PRIVATE/DRAFT；状态切换、编辑和删除使用version，旧版本409。
- 删除采用既有软删除，公开查询始终同时过滤status和deleted。
- 课程关联可选。无课程不能携带章节，无章节不能携带视频时间点；普通关联课程只校验已发布，无需权益。视频位置仍要求课程学习权益。

## 第一批接口

所有接口仍经网关并要求登录，公开指“其他用户可见”，匿名访问后置。

| 操作 | 接口 | 参数/结果 |
|---|---|---|
| 创建草稿 | POST /api/learning/notes | 原DTO，courseId允许null；clientRequestId保持请求幂等 |
| 本人旧列表 | GET /api/learning/notes | 保持旧List契约，供现有课程Note组件使用 |
| 本人分页 | GET /api/learning/notes/mine | courseId可选，keyword可选，pageNo/pageSize |
| 最新公开笔记及搜索 | GET /api/learning/notes/public | 同上；只查询PUBLIC，标题和正文包含搜索 |
| 公开详情 | GET /api/learning/notes/public/{noteId} | 私人、草稿、已删除、不存在均404 |
| 状态切换 | PATCH /api/learning/notes/{noteId}/status | {status:DRAFT/PRIVATE/PUBLIC, version:当前版本} |
| 本人详情/编辑/重命名/删除 | 原接口 | 保留所有者权限和版本控制 |

Note返回追加authorId/status/publishedAt；不返回clientRequestId、播放URL或作者登录凭证。分页pageNo1—1000、pageSize1—50，keyword最大100字符。公开按publishedAt/id倒序，本人按updatedAt/id倒序。搜索参数绑定，并转义LIKE通配字符；不支持全文检索相关性或热门排序。

## 第三批互动设计

- 点赞、收藏分别以`(note_id,user_id)`唯一约束作为业务幂等键。`PUT`重复调用返回当前状态，`DELETE`重复调用同样成功，不要求客户端额外生成UUID。
- 评论创建使用客户端UUID `clientRequestId`，唯一键为`(user_id,client_request_id)`；相同Key绑定相同note、父评论和正文，不同请求指纹返回409。
- 评论支持一级回复引用，`parentCommentId`必须指向同一篇仍有效的评论；首版按创建时间正序分页，不构造无限嵌套树。
- 互动只允许作用于未删除的PUBLIC笔记。撤回后保留既有互动事实，重新发布后恢复展示；删除笔记后公开入口不可访问互动。
- `note`保存like/favorite/comment计数冗余字段，互动事实表和计数在同一个MySQL事务内更新；计数使用行锁串行化笔记级写入，并以唯一约束保证并发正确性。
- 热门排序只使用真实数据，首版确定性顺序为收藏数、点赞数、评论数、发布时间、ID倒序。此口径不是推荐算法，不宣称具备反作弊能力。
- 当前不使用Redis缓存互动计数。数据规模与压测证明MySQL成为瓶颈后，再设计缓存失效、重建和对账，不提前引入双写。

| 操作 | 接口 | 幂等/分页语义 |
|---|---|---|
| 查询互动状态 | GET /api/learning/notes/public/{noteId}/engagement | 返回计数及当前用户是否点赞/收藏 |
| 点赞/取消 | PUT/DELETE /api/learning/notes/public/{noteId}/likes | 自然业务键幂等 |
| 收藏/取消 | PUT/DELETE /api/learning/notes/public/{noteId}/favorites | 自然业务键幂等 |
| 评论列表 | GET /api/learning/notes/public/{noteId}/comments | pageNo 1—1000，pageSize 1—50 |
| 创建评论/回复 | POST /api/learning/notes/public/{noteId}/comments | UUID幂等，正文1—1000字符 |
| 删除本人评论 | DELETE /api/learning/notes/comments/{commentId}?version= | 软删除与乐观锁 |
| 我的点赞/收藏 | GET /api/learning/notes/liked、/favorites | 仅返回仍公开的笔记 |

## 后续范围

- 已交付文字笔记首页卡片流、详情、我的笔记、创作与发布，以及点赞、收藏、一级评论回复、我的点赞/收藏和真实热门排序。
- 图片上传须先明确对象存储、访问授权、格式/大小限制和内容治理后再实现，不把文件塞进数据库。
- 举报、审核、反作弊和推荐算法后置；完成这些治理前不宣称可直接开放互联网生产流量。
- 当前不拆新微服务，继续按业务域分包；互联网生产开放需补齐公开内容治理。

## 本地演示数据

在IAM、学习服务和网关已启动且使用同一JWT密钥时执行：

```bash
node scripts/create-local-note-data.mjs
```

脚本使用本地演示账号，通过登录、创建与状态切换API生成三篇公开文字笔记；不直写数据库，不生成虚假互动或热度。重复执行复用固定业务幂等键，不会重复创建。
