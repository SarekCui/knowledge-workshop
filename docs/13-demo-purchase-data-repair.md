# 本地拼团联调数据修复

## 本次原因及结果（2026-09-14）

- activity-demo关联course-java，但本地learning数据库没有该课程或章节，课程详情404。
- group-demo已经FORMED，confirmed_count/target_count=3/3，因此可用团列表为空；不应重置成团状态。
- 补入course-java为PUBLISHED，新增Oceans本地联调章节（46613ms），不冒充Java教学视频。
- 通过管理API创建group-demo-web-v1：FORMING，0/3；重复执行复用可用团。
- 原group-demo仍FORMED、3/3，原三张course-java订单数量不变；未自动参团、支付、重放历史消息或插入权益。
- API检查课程及可用团成功；视频HEAD=200、Accept-Ranges=bytes。

## 可复现操作

仅本地环境，先执行数据库迁移，准备local-test-admin账号，并启动IAM、网关、learning、marketing和指定视频服务。
在项目根目录执行：

```sh
docker compose exec -T mysql mysql --default-character-set=utf8mb4 -uroot -proot < scripts/repair-demo-course.sql
node scripts/ensure-demo-group.mjs
node --test scripts/ensure-demo-group.test.mjs
```

SQL只INSERT IGNORE缺失课程及联调章节，不修改已有内容、密码、团、订单或权益。管理脚本先检查课程，再查询可用团；创建请求失败后重新运行会先查询同一groupId，不盲目创建随机新团。

## 中文导入编码修复

首次执行修复SQL时没有声明连接编码，UTF-8文本被错误解码后存入数据库，导致课程标题/简介和章节标题乱码。责任在本地导入，不是前端字体问题。

后续SQL使用`SET NAMES utf8mb4`并显式指定客户端字符集。已污染的两条文本通过以下脚本修复：

```sh
docker compose exec -T mysql mysql --default-character-set=utf8mb4 -uroot -proot < scripts/fix-demo-course-encoding.sql
node --test scripts/demo-encoding.test.mjs
```

修复限定明确ID、version=0和原始乱码字节；增加版本号，重复执行不再更新，不覆盖后续编辑。只修复课程标题/简介与章节标题，不变更视频、团、订单或权益。

如果默认测试团已结束且没有其他可用团，脚本明确停止；确需新团时显式指定新资源ID：

```sh
LOCAL_TEST_GROUP_ID=group-demo-web-v2 node scripts/ensure-demo-group.mjs
```

不会自动生成第二个团或放宽用户参与次数限制。活动仍为3人团，需要3个符合规则的不同账号各自参团并模拟支付才能成团。

前端地址：`http://127.0.0.1:5173/activities/activity-demo`。刷新后应显示课程与group-demo-web-v1，参团/支付由用户在前端操作。本轮验证为真实API/数据库/视频HEAD及4个脚本测试，不声明完成浏览器端到端验收。

历史成团但缺失权益的问题需单独核对消息/死信，不在本次修复中自动补发。
