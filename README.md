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
git clone https://github.com/your-org/db-sync-platform.git
cd db-sync-platform
```

2. **启动基础设施**
```bash
cd deployment/docker-compose
docker-compose up -d
```

这将启动：
- PostgreSQL (元数据库)
- Redis (分布式锁/缓存)
- Kafka + Zookeeper
- Kafka Connect
- Prometheus + Grafana

3. **编译项目**
```bash
mvn clean install -DskipTests
```

4. **启动Spring Boot服务**
```bash
cd db-sync-api
mvn spring-boot:run
```

5. **访问服务**
- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- Grafana: http://localhost:3000 (admin/admin)
- Prometheus: http://localhost:9090

### 创建第一个同步任务

```bash
# 1. 创建租户
curl -X POST http://localhost:8080/api/v1/tenants \
  -H "Content-Type: application/json" \
  -d '{
    "tenantName": "Demo Tenant",
    "tenantCode": "demo",
    "maxConnectors": 10
  }'

# 2. 创建同步任务
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

# 3. 启动任务
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
├── deployment/                 # 部署配置
│   ├── docker-compose/         # Docker Compose配置
│   ├── kubernetes/             # Kubernetes YAML
│   └── helm/                   # Helm Charts
│
├── docs/                       # 文档
│   ├── 00-项目概述.md
│   ├── 01-架构设计.md
│   └── ...
│
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

## 常见问题

### Q: 支持哪些数据库版本？
A: MySQL 5.7+, Oracle 11g+, SQL Server 2017+, PostgreSQL 12+

### Q: 如何处理大表同步？
A: 使用Debezium的增量快照功能，详见[任务配置指南](docs/guides/task-configuration.md)

### Q: 是否支持双向同步？
A: 支持，但需要注意循环复制问题，建议使用不同的表或添加标识字段

### Q: 如何保证数据一致性？
A: 利用Debezium的事务元数据功能，在目标端作为事务执行

更多问题请查看 [FAQ](docs/FAQ.md)

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
