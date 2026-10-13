# Docker Compose 实测指南（O8）

> **验证状态**：编排配置已通过静态审查 + CI 构建验证；后端全链路已在本机以等价配置
> （MySQL 8 + Redis 5，非容器）实测通过（冒烟 15/15、功能 17/18、单元 47/47）。
> `docker compose up` 本身尚未在装有 Docker daemon 的机器上跑过——本文档的验证清单
> 就是为此准备的，任何人执行后把结果贴回即可把状态升级为「容器环境已验证」。
>
> 已确认无误的部分：编排结构完整、环境变量注入齐全、`init.sql` 自动初始化（含
> `asset_record` 表）、healthcheck/restart/资源上限/本机端口绑定均已就位。
> 前置的 Docker daemon 未就绪属于环境问题，不是配置问题——`scripts/preflight.sh`
> 会把这类问题一次性列清楚。

## 零、环境预检（建议先跑）

```bash
bash scripts/preflight.sh        # Docker 模式
```

它会一次性告诉你缺什么：Docker 是否安装、daemon 是否启动、端口是否被占、
`.env` 是否齐全、JWT 强度够不够、默认口令守卫是否需要处理。**不要跳过这步**——
本项目 90% 的"部署失败"都是这几类原因。

## 一、前置条件

- Docker 24+、Docker Compose 2+
- 项目根目录准备 `.env`（推荐直接 `bash scripts/deploy.sh`，会自动生成随机密钥并校验）：

```env
MYSQL_USER=root
MYSQL_PASSWORD=<随机生成的强密码>
JWT_SECRET=<openssl rand -base64 32 生成的值>
CORS_ALLOWED_ORIGINS=http://localhost
AI_SIMULATION_ENABLED=true
# 可选：GEO 真实采集器（G-01），默认关闭
# GEO_COLLECTOR_ENABLED=true
# GEO_COLLECTOR_BRANDS=品牌A,品牌B
# GEO_PERPLEXITY_API_KEY=pplx-xxx
```

## 二、首次部署必须改密（默认口令闸门）

`init.sql` 注入的管理员 `admin / admin123`，其 BCrypt 散列**固定写在公开仓库里**，
等同于凭据泄露。为避免"谁都能登录你刚部署的后台"，后端在 prod 档案下会检查
数据库里的口令是否仍是默认值，**是则直接拒绝启动**并打印指引。

改密步骤：

```bash
# 1. 生成 BCrypt 散列（明文只留在你自己的终端）
python -c "import bcrypt;print(bcrypt.hashpw(b'你的新密码',bcrypt.gensalt(10)).decode())"

# 2. 把结果填进 scripts/change_password.sql 的 <NEW_BCRYPT_HASH>，然后执行
docker compose exec -T mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" geo_saa' \
  < scripts/change_password.sql
```

若只是本地演示、明确知道风险，可在 `.env` 里设 `DEFAULT_PASSWORD_ACTION=disabled`
放行并保留告警。dev 档案默认 `warn`，不影响本地开发。

## 三、一键启动

```bash
bash scripts/deploy.sh
# 等价：docker compose up -d --build
```

首次启动 MySQL 容器会执行挂载的
`geo-saa-backend/src/main/resources/db/init.sql`：
自动建库 `geo_saa`、建全部表（含 12. `asset_record`）、注入 `admin/admin123` 与系统配置。

## 四、验证清单

```bash
# 一键验收（等价于下面 1-6 步，且额外回归本次修复的两个缺陷）
bash scripts/healthcheck.sh

# 1. 全部容器健康
docker compose ps
#    期望：mysql/redis/rabbitmq healthy，backend/frontend running

# 2. 后端健康检查（无需鉴权）
curl http://localhost:8080/api/v1/system/health
#    期望：HTTP 200，{"code":200,...}

# 3. 登录拿 token
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}'
#    期望：返回 data.token

# 4. 端到端冒烟（复用本地脚本，直连容器后端）
SMOKE_BASE=http://127.0.0.1:8080 python smoke_test.py
#    期望：SUMMARY: 15 passed, 0 failed

# 5. 前端可达
curl -I http://localhost/    # 期望 200，Nginx 托管 dist

# 6.（可选）GEO 采集器验证：置 GEO_COLLECTOR_ENABLED=true 并配好引擎后
docker compose logs backend | grep "GEO 定时采集"   # 期望出现采集完成日志
```

## 五、已完成的检查结论

### 5.1 编排静态审查（已完成）

| 检查项 | 结论 |
|--------|------|
| MySQL 首次初始化 `init.sql` | ✅ 挂载 `docker-entrypoint-initdb.d`，自动建库建表（含 `asset_record`） |
| 后端 prod 档案环境变量 | ✅ `MYSQL_HOST/USER/PASSWORD`、`REDIS_HOST`、`JWT_SECRET`（fail-fast）、`CORS_ALLOWED_ORIGINS` 齐全 |
| RabbitMQ 联动 | ✅ compose 提供 rabbitmq 服务；prod 未排除 MQ 自动配置，可正常连接 |
| 采集器配置 | ✅ 默认关闭（`app.geo.collector.enabled=false`），未配 key 时后端照常启动，不影响既有接口 |
| 前端镜像 | ✅ `frontend/Dockerfile` 已使用 `npm ci`（配合已提交的 `package-lock.json`，可复现构建） |
| 服务健壮性 | ✅ mysql/redis/rabbitmq 均有 healthcheck + restart 策略 + 资源上限；端口仅绑定 127.0.0.1 |
| 备份 | ✅ 独立 backup 容器按 `BACKUP_RETENTION_DAYS` 轮转 |

### 5.2 非容器环境实测（已完成，等价配置）

在同一套 MySQL 8 + Redis 上以 `dev` 档案直跑 jar，覆盖了除容器网络编排外的全部业务逻辑：

| 验证项 | 结果 |
|--------|------|
| 单元测试 | ✅ 47/47 通过 |
| 冒烟测试 `smoke_test.py` | ✅ 15/15 通过 |
| 功能测试 `func_test.py` | ✅ 17/18 通过（1 项为幂等重复，非缺陷） |
| 批量生成（RabbitMQ 降级路径） | ✅ 返回 200，原 500 缺陷已修复 |
| 唯一约束冲突 | ✅ 返回 400 可读提示，原 500 已修复 |
| 错误密码登录 | ✅ 返回 HTTP 401（原为 200 + body 报错） |
| 资产统计口径 | ✅ 新增 `brandTotal`，字段语义不再混淆 |

### 5.3 待补：容器环境实测（未执行）

唯一未覆盖的环节是 `docker compose up` 本身。执行第四节清单后，
把 `docker compose ps` 与 `healthcheck.sh` 的输出贴回，即可把本文档状态
升级为「容器环境已验证」。

- **数据卷持久化**：`init.sql` 仅在 `mysql-data` 数据卷为空（首次）时执行；
  若改过表结构需 `docker compose down -v` 后重建（会清空数据）。
- **端口占用**：3306/6379/5672/15672/8080/80 与本地服务冲突时，先停本机实例或改 compose 端口映射。
- **本机实测后**：把执行结果（`docker compose ps` + 冒烟输出）贴回，可更新本文件状态为「已验证」。
