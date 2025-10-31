# DB Sync Platform 快速启动指南

## 前置条件

1. ✅ JDK 17 已安装
2. ✅ Maven 3.6+ 已安装
3. ✅ Docker 和 Docker Compose 已安装
4. ✅ IntelliJ IDEA (推荐) 或其他 IDE

## 启动步骤

### 1. 启动 Docker 基础设施

```bash
cd deploy
docker-compose up -d
```

等待所有服务启动完成(约30秒):
- PostgreSQL (端口 5432)
- Redis (端口 6379)
- Kafka (端口 9092)
- Kafka Connect (端口 8083)
- Zookeeper (端口 2181)
- Prometheus (端口 9090)
- Grafana (端口 3000)

### 2. 验证环境

```bash
cd ..
./verify-startup.sh
```

应该看到所有检查项都通过 ✅

### 3. 在 IDEA 中启动应用

#### 方法 A: 直接运行主类

1. 在 IDEA 中打开项目
2. 找到 `db-sync-api/src/main/java/com/dbsync/api/DbSyncApplication.java`
3. 右键点击 `DbSyncApplication` -> Run 'DbSyncApplication'
4. 或者设置 VM options: `-Dspring.profiles.active=dev`

#### 方法 B: 使用 Maven

```bash
mvn spring-boot:run -pl db-sync-api
```

### 4. 验证应用启动

应用启动后,访问以下端点:

- **API文档 (Swagger UI)**: http://localhost:8080/swagger-ui.html ✅ **无需登录!**
- **健康检查**: http://localhost:8080/actuator/health
- **监控指标**: http://localhost:8081/actuator/prometheus
- **Kafka Connect**: http://localhost:8083/

> 📝 **注意**: 开发环境下所有接口都是开放的,无需认证。详见 [SECURITY_NOTE.md](SECURITY_NOTE.md)

### 5. 查看日志

应用日志文件位置: `logs/db-sync-platform.log`

## 问题排查

### 问题 1: 端口占用

如果端口被占用,可以修改配置:
- 应用端口: `db-sync-api/src/main/resources/application.yml` 中的 `server.port`
- Docker服务端口: `deploy/docker-compose.yml` 中的端口映射

### 问题 2: 数据库连接失败

检查 PostgreSQL 是否正常运行:
```bash
docker exec dbsync-postgres psql -U dbsync_user -d dbsync_metadata -c "SELECT 1;"
```

### 问题 3: Bean 定义冲突

✅ 已修复: 删除了 `DbSyncApplication` 中重复的 `@EnableJpaAuditing` 注解

### 问题 4: Kafka Connect 不可用

检查 Kafka Connect 状态:
```bash
curl http://localhost:8083/
```

## 停止服务

### 停止应用
在 IDEA 中点击红色停止按钮,或在终端按 Ctrl+C

### 停止 Docker 服务
```bash
cd deploy
docker-compose down
```

### 清理数据(可选)
```bash
cd deploy
docker-compose down -v  # 删除所有数据卷
```

## 开发配置

### 配置文件说明

- `application.yml`: 主配置文件(默认)
- `application-dev.yml`: 开发环境配置
- `application-prod.yml`: 生产环境配置(待创建)

### 日志级别

开发环境日志级别在 `application-dev.yml` 中配置:
```yaml
logging:
  level:
    root: INFO
    com.dbsync: DEBUG
```

## 已完成功能

✅ Kafka Connect 客户端封装
✅ MySQL Connector 配置构建器
✅ Connector 生命周期管理
✅ 同步任务 Service 层
✅ JPA 实体和 Repository
✅ 基础配置和工具类

## 下一步开发

根据实施路线图 Week 5-6,接下来需要开发:
1. REST API 接口 (租户管理、任务管理)
2. JWT 认证和授权
3. Spring Boot Actuator 监控
4. 端到端集成测试

## 技术栈

- **Spring Boot 3.2.2** - 应用框架
- **Kafka Connect + Debezium** - CDC 数据同步
- **PostgreSQL 15** - 元数据存储
- **Redis 7** - 缓存
- **Kafka 3.6** - 消息队列
- **Prometheus + Grafana** - 监控

## 帮助和支持

如有问题,请查看:
- `docs/` 目录下的详细文档
- `Week3-4-CoreDevelopment-Summary.md` - 核心功能开发总结
- 项目日志文件: `logs/db-sync-platform.log`

---

**最后更新**: 2025-10-31
**项目状态**: Week 3-4 核心功能开发完成 ✅
