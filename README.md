# 异构数据库实时同步平台

[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.2-green.svg)](https://spring.io/projects/spring-boot)
[![Debezium](https://img.shields.io/badge/Debezium-2.5-red.svg)](https://debezium.io/)

## 项目简介

一个基于 **Spring Boot + Debezium CDC** 的通用异构数据库实时同步平台，支持MySQL、Oracle、SQL Server等主流数据库之间的数据同步，具备实时同步、数据转换、断点续传、多租户等企业级特性。

### 核心特性

- **异构数据库支持**：支持MySQL、Oracle、SQL Server、PostgreSQL
- **实时CDC**：基于Debezium的变更数据捕获，毫秒级延迟
- **灵活转换**：字段级映射、数据过滤、自定义脚本转换
- **高可用**：分布式架构，故障自动恢复，断点续传
- **多租户**：支持租户隔离和资源配额管理
- **企业级监控**：完整的监控告警体系

## 架构概览

```
┌─────────────┐         ┌──────────────┐         ┌─────────────┐
│  Source DB  │────────>│   Debezium   │────────>│   Kafka     │
│ (MySQL/     │         │   Connector  │         │   Topics    │
│  Oracle/    │         └──────────────┘         └──────┬──────┘
│  SQLServer) │                                          │
└─────────────┘                                          │
                                                         ▼
                                              ┌──────────────────┐
                                              │   Transform      │
                                              │   Service        │
                                              └────────┬─────────┘
                                                       │
                                                       ▼
┌─────────────┐         ┌──────────────┐         ┌─────────────┐
│  Target DB  │<────────│     Sink     │<────────│   Kafka     │
│ (MySQL/     │         │  Connector   │         │   Topics    │
│  PostgreSQL)│         └──────────────┘         └─────────────┘
└─────────────┘

                    ┌─────────────────────┐
                    │  Spring Boot        │
                    │  Management Service │
                    │  (Task/Config/      │
                    │   Monitoring)       │
                    └─────────────────────┘
                             ▲
                             │
                    ┌────────┴────────┐
                    │   PostgreSQL    │
                    │   (Metadata)    │
                    └─────────────────┘
```

## 快速开始

### 前置要求

- **Java 17+**
- **Maven 3.8+**
- **Docker & Docker Compose**
- **至少 8GB 内存**

### 启动开发环境

1. **克隆项目**
```bash
git clone https://github.com/liumingjian/db-sync-platform.git
cd db-sync-platform
```

2. **启动基础设施**
```bash
# 在项目根目录执行
docker-compose up -d
```

这将启动：
- PostgreSQL (元数据库) - 端口 5432
- Redis (分布式锁/缓存) - 端口 6379
- Kafka + Zookeeper - 端口 9092/9093, 2181
- Kafka Connect - 端口 8083
- Prometheus - 端口 9090
- Grafana - 端口 3000

3. **验证基础设施状态**
```bash
# 查看所有容器状态
docker-compose ps

# 应该看到所有服务都是 healthy 或 running 状态
# 等待约30秒让所有服务完全启动
```

4. **初始化数据库**
```bash
# 数据库会在容器启动时自动初始化
# 验证数据库是否初始化成功
docker exec -it dbsync-postgres psql -U dbsync_user -d dbsync_metadata -c "\dt"

# 应该能看到 tenants 和 sync_tasks 表
```

5. **编译项目**
```bash
# 在项目根目录执行
mvn clean install -DskipTests
```

6. **启动Spring Boot服务**
```bash
# 方式1: 使用Maven
cd db-sync-api
mvn spring-boot:run

# 方式2: 使用Java
java -jar db-sync-api/target/db-sync-api-1.0.0-SNAPSHOT.jar
```

7. **验证服务启动**
```bash
# 等待约30秒后，检查应用健康状态
curl http://localhost:8081/actuator/health

# 预期输出: {"status":"UP"}
```

8. **访问服务**
- **REST API**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **Actuator**: http://localhost:8081/actuator
- **Grafana**: http://localhost:3000 (admin/admin)
- **Prometheus**: http://localhost:9090
- **Kafka Connect**: http://localhost:8083

### 初始化验证清单

完成上述步骤后，请按照以下清单验证项目是否正确初始化：

#### 1. 验证Docker容器状态
```bash
docker-compose ps
```
**预期结果**: 所有服务状态为 `Up` 或 `healthy`
```
NAME                   STATUS
dbsync-postgres        Up (healthy)
dbsync-redis           Up (healthy)
dbsync-kafka           Up (healthy)
dbsync-zookeeper       Up
dbsync-kafka-connect   Up (healthy)
dbsync-prometheus      Up
dbsync-grafana         Up
```

#### 2. 验证PostgreSQL数据库
```bash
# 检查数据库表
docker exec -it dbsync-postgres psql -U dbsync_user -d dbsync_metadata -c "\dt"

# 检查默认租户
docker exec -it dbsync-postgres psql -U dbsync_user -d dbsync_metadata -c "SELECT tenant_name, tenant_code, status FROM tenants;"
```
**预期结果**: 能看到 `tenants` 和 `sync_tasks` 表，且有一个默认租户 "Default Tenant"

#### 3. 验证Redis连接
```bash
docker exec -it dbsync-redis redis-cli ping
```
**预期结果**: 输出 `PONG`

#### 4. 验证Kafka Connect
```bash
curl http://localhost:8083/ | jq
```
**预期结果**: 返回Kafka Connect版本信息

#### 5. 验证Spring Boot应用
```bash
# 健康检查
curl http://localhost:8081/actuator/health | jq

# 检查应用信息
curl http://localhost:8081/actuator/info | jq

# 检查可用的端点
curl http://localhost:8081/actuator | jq
```
**预期结果**:
- health 返回 `{"status":"UP"}`
- 所有端点正常响应

#### 6. 验证Swagger UI
打开浏览器访问: http://localhost:8080/swagger-ui.html

**预期结果**: 能看到API文档界面（目前暂无API接口，等待后续开发）

#### 7. 验证监控系统
```bash
# Prometheus targets
curl http://localhost:9090/api/v1/targets | jq

# Grafana健康检查
curl http://localhost:3000/api/health
```

**预期结果**: Prometheus和Grafana正常运行

### 完整验证脚本

你可以使用以下一键验证脚本：

```bash
#!/bin/bash
echo "=========================================="
echo "DB Sync Platform - 初始化验证"
echo "=========================================="

# 1. Docker容器状态
echo -e "\n[1/7] 检查Docker容器状态..."
docker-compose ps

# 2. PostgreSQL
echo -e "\n[2/7] 检查PostgreSQL数据库..."
docker exec -it dbsync-postgres psql -U dbsync_user -d dbsync_metadata -c "\dt" 2>/dev/null && echo "✓ PostgreSQL正常"

# 3. Redis
echo -e "\n[3/7] 检查Redis..."
docker exec -it dbsync-redis redis-cli ping 2>/dev/null && echo "✓ Redis正常"

# 4. Kafka Connect
echo -e "\n[4/7] 检查Kafka Connect..."
curl -s http://localhost:8083/ > /dev/null && echo "✓ Kafka Connect正常"

# 5. Spring Boot应用
echo -e "\n[5/7] 检查Spring Boot应用..."
HEALTH=$(curl -s http://localhost:8081/actuator/health | grep -o '"status":"UP"')
if [ ! -z "$HEALTH" ]; then
    echo "✓ Spring Boot应用正常"
else
    echo "✗ Spring Boot应用未启动或异常"
fi

# 6. Prometheus
echo -e "\n[6/7] 检查Prometheus..."
curl -s http://localhost:9090/-/healthy > /dev/null && echo "✓ Prometheus正常"

# 7. Grafana
echo -e "\n[7/7] 检查Grafana..."
curl -s http://localhost:3000/api/health > /dev/null && echo "✓ Grafana正常"

echo -e "\n=========================================="
echo "验证完成！"
echo "=========================================="
echo "访问地址："
echo "  - Swagger UI: http://localhost:8080/swagger-ui.html"
echo "  - Actuator:   http://localhost:8081/actuator"
echo "  - Grafana:    http://localhost:3000 (admin/admin)"
echo "  - Prometheus: http://localhost:9090"
echo "=========================================="
```

保存为 `verify.sh` 并执行：
```bash
chmod +x verify.sh
./verify.sh
```

### 创建第一个同步任务

> **注意**: 当前版本为脚手架代码，API接口尚未完全实现。以下示例将在后续版本中可用。

```bash
# 1. 创建租户（待实现）
curl -X POST http://localhost:8080/api/v1/tenants \
  -H "Content-Type: application/json" \
  -d '{
    "tenantName": "Demo Tenant",
    "tenantCode": "demo",
    "maxConnectors": 10
  }'

# 2. 创建同步任务（待实现）
curl -X POST http://localhost:8080/api/v1/tasks \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": "your-tenant-id",
    "taskName": "MySQL to PostgreSQL Sync",
    "taskCode": "mysql-pg-001",
    "sourceDbType": "MYSQL",
    "sourceConnectionConfig": {
      "host": "mysql-source.example.com",
      "port": 3306,
      "database": "mydb",
      "username": "dbuser",
      "password": "password"
    },
    "targetDbType": "POSTGRESQL",
    "targetConnectionConfig": {
      "host": "postgres-target.example.com",
      "port": 5432,
      "database": "target_db",
      "username": "pguser",
      "password": "password"
    },
    "syncMode": "FULL_INCREMENTAL"
  }'

# 3. 启动任务（待实现）
curl -X POST http://localhost:8080/api/v1/tasks/{taskId}/start
```

## 文档

### 设计文档
- [项目概述](docs/00-项目概述.md) - 项目背景、目标和范围
- [架构设计](docs/01-架构设计.md) - 系统架构和组件设计
- [数据库设计](docs/02-数据库设计.md) - 元数据库表结构
- [API设计](docs/03-API设计.md) - REST API接口规范
- [部署架构](docs/04-部署架构.md) - Kubernetes和Docker部署
- [实施路线图](docs/05-实施路线图.md) - 项目实施计划

### 用户手册
- [快速入门指南](docs/guides/quickstart.md)
- [任务配置指南](docs/guides/task-configuration.md)
- [数据转换指南](docs/guides/data-transformation.md)
- [监控运维指南](docs/guides/monitoring.md)

### API文档
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI Spec: http://localhost:8080/v3/api-docs

## 项目结构

```
db-sync-platform/
├── db-sync-common/              # 公共模块
│   ├── enums/                   # 枚举定义
│   ├── constants/               # 常量
│   ├── exceptions/              # 异常定义
│   └── utils/                   # 工具类
│
├── db-sync-core/                # 核心业务模块
│   ├── domain/                  # 领域模型
│   │   ├── entity/             # JPA实体
│   │   ├── dto/                # 数据传输对象
│   │   └── vo/                 # 视图对象
│   ├── repository/             # 数据访问层
│   └── service/                # 业务逻辑层
│
├── db-sync-connector/          # Connector管理模块
│   ├── client/                 # Kafka Connect客户端
│   ├── builder/                # Connector配置构建器
│   └── manager/                # Connector生命周期管理
│
├── db-sync-transform/          # 数据转换模块
│   ├── engine/                 # 转换引擎
│   ├── mapper/                 # 类型映射器
│   └── streams/                # Kafka Streams拓扑
│
├── db-sync-api/                # API接口模块
│   ├── controller/             # REST控制器
│   ├── security/               # 安全配置
│   └── DbSyncApplication.java  # 主启动类
│
├── deploy/                     # 部署目录
│   ├── config/                 # 配置文件
│   │   └── prometheus.yml     # Prometheus配置
│   ├── volumes/                # 数据卷（相对路径，不提交）
│   │   ├── postgres/          # PostgreSQL数据
│   │   ├── redis/             # Redis数据
│   │   ├── zookeeper/         # Zookeeper数据
│   │   ├── kafka/             # Kafka数据
│   │   ├── prometheus/        # Prometheus数据
│   │   └── grafana/           # Grafana数据
│   └── README.md              # 部署说明
│
├── scripts/                    # 脚本目录
│   └── database/               # 数据库脚本
│       └── 01_init_database.sql
│
├── docs/                       # 文档
│   ├── 00-项目概述.md
│   ├── 01-架构设计.md
│   └── ...
│
├── docker-compose.yml          # Docker Compose配置
├── verify.sh                   # 验证脚本
└── pom.xml                     # Maven主配置
```

## 技术栈

| 组件 | 版本 | 说明 |
|------|------|------|
| Java | 17 | JDK版本 |
| Spring Boot | 3.2.2 | 应用框架 |
| Spring Data JPA | 3.2.2 | ORM框架 |
| Spring Kafka | 3.1.1 | Kafka集成 |
| Debezium | 2.5.1 | CDC引擎 |
| Apache Kafka | 3.6.1 | 消息队列 |
| PostgreSQL | 15 | 元数据库 |
| Redis | 7.0 | 缓存/分布式锁 |
| Redisson | 3.26.0 | Redis客户端 |
| Prometheus | - | 监控指标 |
| Grafana | - | 可视化 |

## 支持的数据库

### 源数据库（Source）
- ✅ MySQL 5.7+
- ✅ Oracle 11g+
- ✅ SQL Server 2017+
- ✅ PostgreSQL 12+

### 目标数据库（Sink）
- ✅ MySQL 5.7+
- ✅ Oracle 11g+
- ✅ SQL Server 2017+
- ✅ PostgreSQL 12+

## 性能指标

| 指标 | 目标值 |
|------|--------|
| 单任务吞吐量 | 10,000+ TPS |
| 端到端延迟（P99） | < 1秒 |
| 并发任务数 | 1000+ |
| 可用性 | 99.9% |
| 故障恢复时间（RTO） | < 5分钟 |

## 监控指标

平台提供丰富的监控指标：

- **业务指标**：任务数、同步记录数、成功率
- **性能指标**：TPS、延迟、Consumer Lag
- **资源指标**：CPU、内存、网络、磁盘
- **告警**：高延迟、高错误率、Connector失败

访问 Grafana Dashboard: http://localhost:3000

## 开发指南

### 代码规范
- 遵循阿里巴巴Java开发手册
- 使用Google Java Style
- 保持代码覆盖率 > 80%

### 分支管理
- `main`: 生产分支
- `develop`: 开发分支
- `feature/*`: 功能分支
- `hotfix/*`: 紧急修复分支

### 提交规范
```
<type>(<scope>): <subject>

<body>

<footer>
```

类型（type）：
- feat: 新功能
- fix: 修复bug
- docs: 文档更新
- style: 代码格式调整
- refactor: 重构
- test: 测试相关
- chore: 构建/工具链相关

### 运行测试
```bash
# 单元测试
mvn test

# 集成测试
mvn verify -P integration-test

# 代码覆盖率
mvn clean test jacoco:report
```

## 部署

### Docker部署
```bash
cd deployment/docker-compose
docker-compose up -d
```

### Kubernetes部署
```bash
# 使用Helm
helm install db-sync ./deployment/helm/db-sync-platform \
  --namespace db-sync-prod \
  --create-namespace

# 或直接使用kubectl
kubectl apply -f deployment/kubernetes/
```

详见 [部署架构文档](docs/04-部署架构.md)

## 贡献指南

我们欢迎所有形式的贡献！

1. Fork本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建Pull Request

详见 [CONTRIBUTING.md](CONTRIBUTING.md)

## 常见问题与故障排查

### 常见问题

#### Q: 支持哪些数据库版本？
A: MySQL 5.7+, Oracle 11g+, SQL Server 2017+, PostgreSQL 12+

#### Q: 如何处理大表同步？
A: 使用Debezium的增量快照功能，详见[任务配置指南](docs/guides/task-configuration.md)

#### Q: 是否支持双向同步？
A: 支持，但需要注意循环复制问题，建议使用不同的表或添加标识字段

#### Q: 如何保证数据一致性？
A: 利用Debezium的事务元数据功能，在目标端作为事务执行

### 故障排查

#### 1. Docker容器启动失败

**问题**: `docker-compose up -d` 后某些容器未启动

**解决方案**:
```bash
# 查看容器日志
docker-compose logs [服务名]

# 常见原因：
# - 端口占用：修改docker-compose.yml中的端口映射
# - 内存不足：确保至少有8GB可用内存
# - 磁盘空间不足：清理Docker镜像和容器

# 重启所有服务
docker-compose down
docker-compose up -d
```

#### 2. Kafka Connect 无法启动

**问题**: Kafka Connect 一直重启或健康检查失败

**解决方案**:
```bash
# 查看Kafka Connect日志
docker-compose logs kafka-connect

# 确认Kafka已完全启动
docker-compose logs kafka | grep "started (kafka.server.KafkaServer)"

# 等待更长时间（Kafka启动可能需要1-2分钟）
sleep 60
docker-compose ps
```

#### 3. Spring Boot应用无法连接数据库

**问题**: 应用启动时报数据库连接错误

**解决方案**:
```bash
# 1. 确认PostgreSQL容器运行正常
docker exec -it dbsync-postgres pg_isready -U dbsync_user

# 2. 检查数据库连接配置
cat db-sync-api/src/main/resources/application.yml | grep -A 5 datasource

# 3. 手动测试数据库连接
docker exec -it dbsync-postgres psql -U dbsync_user -d dbsync_metadata -c "SELECT 1"

# 4. 查看应用日志
tail -f logs/db-sync-platform.log
```

#### 4. Maven编译失败

**问题**: `mvn clean install` 报错

**解决方案**:
```bash
# 检查Java版本
java -version  # 必须是Java 17+

# 检查Maven版本
mvn -version   # 必须是Maven 3.8+

# 清理Maven本地仓库缓存
rm -rf ~/.m2/repository/com/dbsync

# 使用国内镜像（如果网络慢）
# 编辑 ~/.m2/settings.xml 添加阿里云镜像

# 跳过测试编译
mvn clean install -DskipTests -X
```

#### 5. 端口冲突

**问题**: 端口已被占用

**解决方案**:
```bash
# 检查端口占用（macOS/Linux）
lsof -i :5432  # PostgreSQL
lsof -i :6379  # Redis
lsof -i :9092  # Kafka
lsof -i :8080  # API
lsof -i :8081  # Actuator

# 修改docker-compose.yml中的端口映射，例如：
# ports:
#   - "15432:5432"  # 将PostgreSQL映射到15432端口
```

#### 6. Swagger UI 无法访问

**问题**: 访问 http://localhost:8080/swagger-ui.html 返回404

**原因**: 当前版本为脚手架代码，Controller层尚未实现

**解决方案**:
- 确认应用已启动: `curl http://localhost:8081/actuator/health`
- 检查Actuator端点: `curl http://localhost:8081/actuator`
- Swagger将在Controller实现后自动可用

#### 7. 清理并重新开始

如果遇到无法解决的问题，可以完全重置环境：

```bash
# 1. 停止并删除所有容器
docker-compose down -v

# 2. 清理Maven构建
mvn clean

# 3. 删除日志文件
rm -rf logs/

# 4. 重新启动
docker-compose up -d
mvn clean install -DskipTests
cd db-sync-api && mvn spring-boot:run
```

### 获取帮助

如果以上方法都无法解决问题，请：

1. 查看项目Issues: https://github.com/liumingjian/db-sync-platform/issues
2. 提交新Issue，并提供：
   - 错误信息和堆栈跟踪
   - 操作系统和版本
   - Java和Maven版本
   - Docker版本
   - 完整的日志文件

更多问题请查看 [FAQ](docs/FAQ.md)（待完善）

## 许可证

本项目采用 [Apache License 2.0](LICENSE) 许可证。

## 联系我们

- 项目主页: https://github.com/your-org/db-sync-platform
- Issues: https://github.com/your-org/db-sync-platform/issues
- 邮件: support@example.com

## 致谢

感谢以下开源项目：
- [Debezium](https://debezium.io/) - CDC引擎
- [Apache Kafka](https://kafka.apache.org/) - 消息队列
- [Spring Boot](https://spring.io/projects/spring-boot) - 应用框架

---

**注意**：本项目目前处于活跃开发阶段，API可能会有变化。生产环境使用前请充分测试。

**版本**: v1.0.0-SNAPSHOT
**最后更新**: 2025-01-30
