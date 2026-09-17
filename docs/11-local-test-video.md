# 本地测试视频

使用用户提供的oceans.mp4，不下载、不修改、不复制原文件、不提交视频到Git。来源授权由素材提供者确认；仅用于本机开发测试，不对外分发。

## 启动

在仓库根目录执行：

```bash
node scripts/serve-test-video.mjs /Users/sarek/Downloads/Video/oceans.mp4
```

地址：http://127.0.0.1:8090/oceans.mp4。

这是无外部依赖的本地测试文件工具，不是新增微服务或生产视频分发服务。仅绑定loopback，只允许访问命令指定的单个文件，不开放下载目录、不支持上传，不绕过业务API的课程权益检查；测试媒体地址本身仅限本机公开。

支持GET、HEAD与单Range请求（206），越界416；退出Ctrl+C。保留原文件不变，修改文件后需重启工具。

## 章节数据

实际ffprobe结果：H.264视频、AAC音频、960×400，23014356字节，时长46.613333秒。

```json
{
  "videoUrl": "http://127.0.0.1:8090/oceans.mp4",
  "videoDurationMs": 46613,
  "videoVersion": 1
}
```

这里仅展示创建章节参数中的媒体字段，不是完整DTO；课程、章节和权益仍需通过实际后台/购买流程创建。前端不写死视频地址。

46秒可测试10秒心跳、暂停保存、拖动排除和重开恢复；不足以覆盖长时间断网或租约过期。不要以循环播放代替新的真实观看区间，更不能虚报视频时长。

## 检查

```bash
node --test scripts/serve-test-video.test.mjs
curl -I http://127.0.0.1:8090/oceans.mp4
curl -s -D - -H 'Range: bytes=0-1023' -o /dev/null http://127.0.0.1:8090/oceans.mp4
```

这些单独检查只验证文件访问与字节区间。真实课程播放验证记录见`docs/acceptance/round-5.6-learning-web.md`。

## 创建真实链路测试数据

先启动MySQL、Redis、RabbitMQ、Nacos、IAM、学习服务、营销服务与网关，以及上面的本地视频工具。所有服务使用相同的JWT_SECRET。在仓库根目录执行：

```bash
docker compose exec -T mysql mysql -uroot -proot < scripts/local-test-accounts.sql
node scripts/create-local-learning-data.mjs
```

仅用于本地开发：新增`local-test-admin`管理员与`local-test-peer`学习者，密码均为`Knowledge@123`，不修改已有`demo`账号的权限。不要在生产环境执行，也不要将这些账号对外开放。

脚本通过管理API创建课程和章节，两个学习账号参团、模拟支付，再等待真实MQ消费发放权益，不直接插入课程权益。已生成对象保存在Git忽略的`data/local-oceans-data.json`，重复执行复用对象和支付流水；不保存令牌、密码或视频。

打开http://127.0.0.1:5173，使用`demo` / `Knowledge@123`登录，选择“Oceans 视频学习测试”→“查看章节”→“打开视频”。按`docs/12-web-auth-and-routing.md`配置浏览器Cookie后，刷新课程页可恢复登录与章节；再打开视频可恢复后端保存的进度。

不要删除清单后盲目重建；活动有7天期限。创建接口超时意味着结果可能未知，应先核查数据库/接口，恢复清单后继续，不能靠自动重试保证创建幂等。该脚本不是生产初始化或支付工具。
