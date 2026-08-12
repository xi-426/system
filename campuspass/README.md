# CampusPass 校园活动预约与签到平台

CampusPass 解决校园讲座、竞赛和志愿活动分散发布、人工登记、满员后无法有序
候补、到场统计困难的问题。

组织者可以创建并发布活动，学生可以预约；满员后系统按先来后到进入候补名单。
有人取消时，系统自动递补第一位候补学生并发送通知。活动现场由组织者生成十分钟
有效的签到 Token，预约成功的学生签到后，组织者可以查看到场数据。

## 技术栈

- Java 17、Spring Boot 3.5
- Spring Security、JWT、BCrypt
- MyBatis-Plus、MySQL 8、Flyway
- Redis
- RabbitMQ
- JUnit 5、Mockito
- Docker Compose、Nginx、Vue 3 简单演示页

这是一个单体后端项目，不使用 Spring Cloud。重点放在 Java 后端岗位常问且能真正
讲清楚的内容：事务、并发安全、数据库约束、缓存过期、消息队列和权限认证。

## 核心业务流程

```text
组织者创建草稿 → 发布活动
                    ↓
              学生发起预约
                    ↓
       数据库条件 UPDATE 原子抢占名额
              ↙              ↘
        抢到名额             活动已满
        RESERVED          WAITLISTED
                              ↓
                    已预约学生取消
                              ↓
                  锁定最早候补记录
                              ↓
                  自动递补为 RESERVED
                              ↓
                 RabbitMQ 异步发送通知
                              ↓
                  Redis 临时 Token 签到
                              ↓
                    组织者查看统计
```

## 为什么不会超卖

名额不是先查再改，而是交给 MySQL 在一条 SQL 中判断并更新：

```sql
UPDATE activity
SET registered_count = registered_count + 1
WHERE id = ?
  AND registered_count < capacity;
```

一条 `UPDATE` 在数据库中原子执行。受影响行数为 1 表示抢到名额，为 0 表示已经
满员。`activity_registration(activity_id, user_id)` 的联合唯一索引同时防止同一
学生重复预约。

取消和候补递补位于同一个事务。递补时通过 `SELECT ... FOR UPDATE` 锁住最早的
候补记录，避免两个取消请求递补同一个人。

## Redis 和 RabbitMQ 在哪里使用

- Redis 保存 `checkin:token:{token}`，值为活动 ID，TTL 为十分钟。生成和使用
  Token 时都会校验“活动开始前 30 分钟至活动结束”的签到窗口；临时签到码天然适合
  用 Redis 过期机制，不需要数据库定时清理。
- 预约、候补、取消和递补事务提交后，事件发送到 RabbitMQ。消费者异步生成站内
  通知，减少主流程等待时间。
- 每条消息有唯一 `eventId`。消费者先检查 `message_consume_record`，数据库也有
  唯一索引，避免同一消息重复生成通知。

当前实现采用“事务提交后发送消息”，没有实现事务消息或 Outbox。因此面试时应如实
表述：已解决消费者重复消费问题，但极端情况下数据库提交成功而 MQ 发送失败仍可能
丢通知；生产系统可继续演进为本地消息表/Outbox。

## 权限设计

- `ORGANIZER`：创建、发布活动，生成签到 Token，查看活动统计。
- `STUDENT`：预约、候补、取消、签到，查看自己的预约、候补和通知。
- 登录成功后签发 JWT；请求经过 Spring Security 过滤链完成身份认证和角色鉴权。
- JWT 包含唯一 `jti`；退出时以剩余有效期为 TTL 写入 Redis 黑名单，旧 Token 再次
  请求会被拒绝。
- 密码使用 BCrypt 摘要保存，不保存明文。

## 定时任务

每分钟扫描一次活动状态：

- 活动开始前一小时向预约成功的学生发送提醒。
- 报名截止后关闭报名。
- 到达活动时间后切换为进行中，结束后切换为已结束。
- 活动结束后将未签到的预约记录标记为缺席。

## 一键启动

环境要求：已安装并启动 Docker Desktop。

```powershell
cd campuspass
Copy-Item .env.example .env
docker compose up --build -d
```

`.env` 保存本地数据库、RabbitMQ 和 JWT 配置，已加入 `.gitignore`，不会提交到
Git。`.env.example` 只包含可公开的本地演示占位值；部署前必须替换为随机强密码和
至少 32 字符的随机 JWT Secret。

首次启动需要下载 Maven 依赖和 Docker 镜像，后续会使用缓存。

启动后：

- 演示页面：<http://localhost:3000>
- 后端接口：<http://localhost:8080>
- RabbitMQ 管理页：<http://localhost:15673>
  - 用户名：读取 `.env` 中的 `RABBITMQ_USERNAME`
  - 密码：读取 `.env` 中的 `RABBITMQ_PASSWORD`
- MySQL：`localhost:3307`
- Redis：`localhost:6380`

演示账号密码均为 `CampusPass123!`：

| 账号 | 角色 | 用途 |
| --- | --- | --- |
| `organizer` | ORGANIZER | 创建、发布、统计 |
| `student` | STUDENT | 预约、取消、签到 |
| `student2` | STUDENT | 候补和自动递补 |

常用命令：

```powershell
docker compose ps
docker compose logs -f app
docker compose down
```

`docker compose down` 只停止并删除容器，不会删除 MySQL、Redis 和 RabbitMQ 的
命名卷数据。不要额外加 `-v`，除非确定要清空演示数据。

## 运行测试

本机不需要安装 Maven，可以直接使用 Maven Docker 镜像：

```powershell
docker run --rm `
  -v campuspass-maven-cache:/root/.m2 `
  -v "${PWD}:/workspace" `
  -w /workspace `
  maven:3.9.11-eclipse-temurin-17 mvn test
```

当前共 24 个单元测试，覆盖活动发布、登录认证、JWT 黑名单、预约、重复预约、候补、取消、
重新预约、自动递补、Redis 签到和 RabbitMQ 消费幂等。

## 主要接口

| 方法 | 路径 | 角色 | 说明 |
| --- | --- | --- | --- |
| POST | `/api/auth/login` | 公开 | 登录并获取 JWT |
| POST | `/api/auth/logout` | 登录用户 | 退出并拉黑当前 JWT |
| GET | `/api/activities` | 公开 | 查询已发布活动 |
| POST | `/api/activities` | 组织者 | 创建活动草稿 |
| POST | `/api/activities/{id}/publish` | 组织者 | 发布活动 |
| POST | `/api/activities/{id}/registrations` | 学生 | 预约或进入候补 |
| DELETE | `/api/activities/{id}/registrations/me` | 学生 | 取消预约 |
| DELETE | `/api/activities/{id}/registrations/waitlist/me` | 学生 | 取消候补 |
| POST | `/api/activities/{id}/checkin-token` | 组织者 | 生成签到 Token |
| POST | `/api/checkins` | 学生 | 签到 |
| GET | `/api/me/notifications` | 登录用户 | 我的通知 |
| GET | `/api/activities/{id}/stats` | 组织者 | 报名及到场统计 |

## 项目结构

```text
src/main/java/com/yan/campuspass
├─ activity       活动发布、状态机、定时任务
├─ auth           登录
├─ security       JWT 认证与权限配置
├─ registration   预约、取消、名额控制
├─ waitlist       候补和自动递补
├─ notification   RabbitMQ 通知和消费幂等
├─ checkin        Redis 签到 Token 与签到
├─ dashboard      学生数据和组织者统计
└─ common         统一响应与异常处理
```

Flyway 脚本位于 `src/main/resources/db/migration`，按 V1 到 V6 管理数据库结构。
分块学习材料位于 `docs`，建议按编号顺序复述，最后使用
`08-mock-interview.md` 完成口述验收。
