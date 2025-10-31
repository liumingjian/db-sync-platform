#!/bin/bash

echo "=== DB Sync Platform 启动验证 ==="
echo ""

# 检查 Docker 服务
echo "1. 检查 Docker 服务状态..."
cd deploy
docker-compose ps | grep -E "(postgres|redis|kafka|kafka-connect)" | grep -c "Up"
RUNNING_SERVICES=$(docker-compose ps | grep -E "(postgres|redis|kafka|kafka-connect)" | grep -c "Up")
echo "   运行中的服务数量: $RUNNING_SERVICES/4"

if [ $RUNNING_SERVICES -eq 4 ]; then
    echo "   ✅ 所有必需的Docker服务正常运行"
else
    echo "   ❌ 部分Docker服务未运行,请先启动: cd deploy && docker-compose up -d"
    exit 1
fi

echo ""
echo "2. 测试数据库连接..."
docker exec dbsync-postgres psql -U dbsync_user -d dbsync_metadata -c "SELECT 1;" > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo "   ✅ PostgreSQL 连接成功"
else
    echo "   ❌ PostgreSQL 连接失败"
    exit 1
fi

echo ""
echo "3. 测试 Redis 连接..."
docker exec dbsync-redis redis-cli ping > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo "   ✅ Redis 连接成功"
else
    echo "   ❌ Redis 连接失败"
    exit 1
fi

echo ""
echo "4. 测试 Kafka Connect API..."
curl -s http://localhost:8083/ > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo "   ✅ Kafka Connect API 可访问"
else
    echo "   ❌ Kafka Connect API 不可访问"
    exit 1
fi

echo ""
echo "5. 编译项目..."
cd ..
mvn clean compile -DskipTests -q
if [ $? -eq 0 ]; then
    echo "   ✅ 项目编译成功"
else
    echo "   ❌ 项目编译失败"
    exit 1
fi

echo ""
echo "=== 环境验证完成 ==="
echo ""
echo "现在可以在 IDEA 中运行 DbSyncApplication 主类了!"
echo "或者使用命令: mvn spring-boot:run -pl db-sync-api"
