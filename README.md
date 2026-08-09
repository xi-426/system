# CampusPass 校园活动预约与签到平台

CampusPass 是一个面向校园讲座、竞赛和志愿活动的预约签到平台，覆盖活动发布、在线预约、满员候补、取消递补、消息提醒、现场签到和数据统计完整链路。

项目重点不是普通管理类 CRUD，而是通过数据库约束、事务、行锁、Redis 和 RabbitMQ 解决预约并发、候补顺序、Token 撤销、重复签到和消息重复消费等问题。

## 技术栈

- Java 17、Spring Boot
- Spring Security、JWT
- MyBatis-Plus、MySQL 8.4、Flyway
- Redis 7.4
- RabbitMQ 4.1
- Spring Scheduling
- JUnit 5、Mockito
- Docker Compose、Nginx、Vue 3

## 核心功能

- 组织者创建活动草稿并发布活动。
- 学生在线预约，满员后自动进入候补名单。
- 正式预约取消后，按候补加入顺序自动递补。
- Spring Security + JWT 实现学生和组织者角色权限。
- Redis 黑名单实现退出后当前 JWT 即时失效。
- RabbitMQ 异步生成预约、候补、递补和提醒通知。
- Redis 保存带 TTL 的临时签到 Token。
- 条件更新防止无资格或重复签到。
- 定时任务自动关闭报名、推进活动状态、发送提醒和标记缺席。
- 组织者查看活动报名数、签到数和到场率。

## 技术亮点

### 并发预约

使用带状态、报名时间和容量条件的 MySQL UPDATE 原子占用名额，保证 registered_count 不超过 capacity。

activity_registration 表通过 activity_id、user_id 联合唯一索引防止同一学生重复预约。容量控制和用户唯一约束分别维护不同的业务不变量。

### 取消与候补递补

在同一个数据库事务中完成：

1. 将正式预约从 RESERVED 更新为 CANCELLED。
2. 使用 SELECT ... FOR UPDATE 锁定候补第一名。
3. 将候补状态更新为 PROMOTED。
4. 创建或恢复正式预约。
5. 无候补用户时才释放活动名额。

### JWT 与 Redis 黑名单

后端验证 JWT 签名、过期时间和角色权限。用户退出时，将 Token 的 jti 写入 Redis，TTL 等于 Token 剩余有效期，使当前 Token 即时失效。

### RabbitMQ 消费幂等

业务事务提交后发送通知事件。消费者使用唯一 eventId、消费记录表和数据库唯一索引防止相同事件重复生成通知。

### Redis 签到

签到 Token 保存到 Redis 并设置 10 分钟 TTL。最终签到资格由 MySQL 中 RESERVED → CHECKED_IN 的条件更新判断，签到表联合唯一索引进一步防止重复签到。

## 一键启动

环境要求：已安装并启动 Docker Desktop。

~~~powershell
cd campuspass
Copy-Item .env.example .env
docker compose up --build -d
~~~

启动后访问：

- 演示页面：http://localhost:3000
- 后端接口：http://localhost:8080
- RabbitMQ 管理页：http://localhost:15673

本地数据库、RabbitMQ 和 JWT 配置保存在 campuspass/.env。该文件已被 Git 忽略，可公开的配置模板为 campuspass/.env.example。

## 演示账号

演示账号密码均为 CampusPass123!。

| 账号 | 角色 | 用途 |
| --- | --- | --- |
| organizer | ORGANIZER | 创建、发布和查看统计 |
| student | STUDENT | 预约、取消和签到 |
| student2 | STUDENT | 候补和自动递补 |

## 项目结构

~~~text
campuspass/
├─ src/main/java/           Java 业务代码
├─ src/main/resources/      配置与 Flyway 数据库迁移
├─ src/test/java/           JUnit/Mockito 单元测试
├─ frontend/                Vue 3 演示页面
├─ compose.yaml             五服务 Docker Compose 编排
├─ Dockerfile               Spring Boot 多阶段构建
└─ README.md                完整项目说明
~~~

## 测试

当前包含 24 个 JUnit/Mockito 单元测试，覆盖活动、登录、JWT、黑名单、预约、候补递补、签到、通知幂等和定时任务等核心分支。

~~~powershell
cd campuspass
docker run --rm -v ${PWD}:/workspace -w /workspace maven:3.9.11-eclipse-temurin-17 mvn test
~~~

## 详细说明

完整的接口、业务规则、状态说明和运行方式请查看：

[CampusPass 项目 README](campuspass/README.md)
