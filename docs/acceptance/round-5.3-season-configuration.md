# 第 5.3 轮第一批：赛季配置

## 范围

新增 `season` 表及 V3 增量迁移，提供以下 ADMIN 专用接口：

- `POST /api/points/admin/seasons`：输入 `season`、`name`，UTC 起止时间按自然季度推导。
- `GET /api/points/admin/seasons/{season}`：配置详情。
- `GET /api/points/admin/seasons?pageNo=1&pageSize=20`：开始时间倒序分页，每页最多 100 条。

创建使用资源自然键 `season`，数据库主键保证并发唯一；同一季度及规范化名称返回原配置（包括创建时间），不同名称返回冲突。季度边界固定，因此不会重叠。不支持配置任意时间窗口。

## 验证入口

- `SeasonPeriodRuleTest`：跨年季度边界、UTC 转换、名称去空格、非法季度及 MySQL DATETIME 年份边界。
- `PointsAcceptanceIT.adminCanCreateAndQueryQuarterSeasonsWithIdempotencyAndAuthorization`：真实 MySQL 迁移、重复创建回放、冲突、详情、分页、分页上限、非法季度和普通用户禁止访问。
- 其余积分消息消费、排行榜重建及快照/归档幂等回归保持。

## 执行结果（2026-09-14）

`JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home mvn -Dmaven.repo.local=.m2/repository -pl knowledge-points -am -Pacceptance verify` 通过。积分模块 5 个单元测试、4 个真实 MySQL / Redis / RabbitMQ 集成验收测试全部通过，无跳过。仅声明本批赛季配置通过验收，未执行全仓库验收。

## 后续范围

本记录只验收赛季配置。后续人工结算、失败重试、晚到积分处理及已结算历史榜单见 `round-5.3-season-settlement.md`。创建赛季配置不会自动建流水分表，运营不能据此认为任意年份已经可以发放积分。
