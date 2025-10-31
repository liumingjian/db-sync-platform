# Deploy 部署目录

本目录包含所有与部署相关的配置和数据存储。

## 目录结构

```
deploy/
├── config/                    # 配置文件
│   └── prometheus.yml        # Prometheus 监控配置
├── volumes/                   # 数据卷（使用相对路径）
│   ├── postgres/             # PostgreSQL 数据文件
│   ├── redis/                # Redis 持久化数据
│   ├── zookeeper/            # Zookeeper 数据
│   │   ├── data/            # Zookeeper 数据
│   │   └── logs/            # Zookeeper 日志
│   ├── kafka/                # Kafka 数据文件
│   ├── prometheus/           # Prometheus 时序数据
│   └── grafana/              # Grafana 配置和数据
└── README.md                 # 本文件
```

## 为什么使用相对路径？

1. **避免权限问题**: 相对路径避免了不同操作系统和用户的权限差异
2. **可移植性**: 项目可以在不同机器上无需修改路径即可运行
3. **易于清理**: 删除 `deploy/volumes` 目录即可清空所有数据
4. **版本控制**: volumes 目录已在 .gitignore 中忽略，不会提交到仓库

## 数据持久化

所有服务的数据都存储在 `volumes/` 目录下：

- **PostgreSQL**: 元数据库数据存储在 `volumes/postgres/`
- **Redis**: AOF 持久化文件存储在 `volumes/redis/`
- **Zookeeper**: 数据和日志存储在 `volumes/zookeeper/{data,logs}/`
- **Kafka**: 消息数据存储在 `volumes/kafka/`
- **Prometheus**: 监控指标数据存储在 `volumes/prometheus/`
- **Grafana**: Dashboard 配置存储在 `volumes/grafana/`

## 清理数据

### 清理所有数据

```bash
# 停止所有容器
docker-compose down

# 删除所有数据
rm -rf deploy/volumes/*

# 重新创建目录结构
mkdir -p deploy/volumes/{postgres,redis,zookeeper/{data,logs},kafka,prometheus,grafana}

# 重新启动
docker-compose up -d
```

### 清理特定服务数据

```bash
# 停止容器
docker-compose stop postgres

# 删除 PostgreSQL 数据
rm -rf deploy/volumes/postgres/*

# 重启容器
docker-compose start postgres
```

## 配置文件

### Prometheus 配置

`config/prometheus.yml` 包含监控目标配置：

- Spring Boot API 监控（Actuator + Prometheus）
- Kafka Connect 监控
- Prometheus 自监控

修改配置后需要重启 Prometheus：

```bash
docker-compose restart prometheus
```

## 备份和恢复

### 备份数据

```bash
# 备份所有数据
tar -czf db-sync-backup-$(date +%Y%m%d).tar.gz deploy/volumes/

# 备份 PostgreSQL 数据
tar -czf postgres-backup-$(date +%Y%m%d).tar.gz deploy/volumes/postgres/
```

### 恢复数据

```bash
# 停止服务
docker-compose down

# 解压备份
tar -xzf db-sync-backup-20250130.tar.gz

# 启动服务
docker-compose up -d
```

## 权限说明

Docker 容器内的服务会以特定用户身份运行，可能会在 volumes 目录下创建文件。这些文件的所有者可能是容器内的用户（如 postgres、redis 等）。

如果遇到权限问题，可以：

```bash
# 方式1: 修改目录权限（推荐）
chmod -R 777 deploy/volumes/

# 方式2: 修改所有者（需要 root 权限）
sudo chown -R $(id -u):$(id -g) deploy/volumes/
```

## 注意事项

1. **不要提交 volumes 目录**: 已在 .gitignore 中配置，包含敏感数据和大文件
2. **定期备份**: 生产环境建议定期备份 volumes 目录
3. **磁盘空间**: Kafka 和 Prometheus 数据会持续增长，注意监控磁盘空间
4. **安全性**: volumes 目录可能包含敏感数据，注意访问权限控制
