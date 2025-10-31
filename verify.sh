#!/bin/bash

# DB Sync Platform - 初始化验证脚本
# 用于验证项目环境是否正确配置和启动

set -e

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 打印带颜色的消息
print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

print_info() {
    echo -e "ℹ $1"
}

echo "=========================================="
echo "DB Sync Platform - 初始化验证"
echo "=========================================="

# 计数器
total_checks=7
passed_checks=0
failed_checks=0

# 1. Docker容器状态
echo -e "\n[1/$total_checks] 检查Docker容器状态..."
if docker-compose ps | grep -q "Up"; then
    print_success "Docker Compose已启动"
    docker-compose ps
    ((passed_checks++))
else
    print_error "Docker Compose未启动或容器异常"
    docker-compose ps
    ((failed_checks++))
fi

# 2. PostgreSQL
echo -e "\n[2/$total_checks] 检查PostgreSQL数据库..."
if docker exec dbsync-postgres psql -U dbsync_user -d dbsync_metadata -c "\dt" &>/dev/null; then
    print_success "PostgreSQL正常"
    docker exec dbsync-postgres psql -U dbsync_user -d dbsync_metadata -c "SELECT COUNT(*) as tenant_count FROM tenants;" 2>/dev/null | grep -A 2 "tenant_count"
    ((passed_checks++))
else
    print_error "PostgreSQL连接失败"
    ((failed_checks++))
fi

# 3. Redis
echo -e "\n[3/$total_checks] 检查Redis..."
if docker exec dbsync-redis redis-cli ping 2>/dev/null | grep -q "PONG"; then
    print_success "Redis正常"
    ((passed_checks++))
else
    print_error "Redis连接失败"
    ((failed_checks++))
fi

# 4. Kafka Connect
echo -e "\n[4/$total_checks] 检查Kafka Connect..."
if curl -s http://localhost:8083/ >/dev/null 2>&1; then
    print_success "Kafka Connect正常"
    echo "Kafka Connect Version: $(curl -s http://localhost:8083/ | grep -o '"version":"[^"]*"' | cut -d'"' -f4 2>/dev/null || echo 'unknown')"
    ((passed_checks++))
else
    print_error "Kafka Connect未启动或无法访问"
    print_warning "Kafka Connect可能需要更长时间启动，请等待1-2分钟后重试"
    ((failed_checks++))
fi

# 5. Spring Boot应用
echo -e "\n[5/$total_checks] 检查Spring Boot应用..."
if curl -s http://localhost:8081/actuator/health >/dev/null 2>&1; then
    HEALTH=$(curl -s http://localhost:8081/actuator/health)
    if echo "$HEALTH" | grep -q '"status":"UP"'; then
        print_success "Spring Boot应用正常运行"
        echo "$HEALTH" | grep -o '"status":"[^"]*"'
        ((passed_checks++))
    else
        print_warning "Spring Boot应用已启动但健康检查未通过"
        echo "$HEALTH"
        ((passed_checks++))
    fi
else
    print_error "Spring Boot应用未启动"
    print_info "请确保已执行: cd db-sync-api && mvn spring-boot:run"
    ((failed_checks++))
fi

# 6. Prometheus
echo -e "\n[6/$total_checks] 检查Prometheus..."
if curl -s http://localhost:9090/-/healthy >/dev/null 2>&1; then
    print_success "Prometheus正常"
    ((passed_checks++))
else
    print_warning "Prometheus未启动或无法访问"
    ((failed_checks++))
fi

# 7. Grafana
echo -e "\n[7/$total_checks] 检查Grafana..."
if curl -s http://localhost:3000/api/health >/dev/null 2>&1; then
    print_success "Grafana正常"
    ((passed_checks++))
else
    print_warning "Grafana未启动或无法访问"
    ((failed_checks++))
fi

# 总结
echo -e "\n=========================================="
echo "验证完成！"
echo "=========================================="
echo "通过检查: $passed_checks/$total_checks"
echo "失败检查: $failed_checks/$total_checks"

if [ $failed_checks -eq 0 ]; then
    print_success "所有检查通过！系统运行正常"
elif [ $passed_checks -ge 5 ]; then
    print_warning "部分检查通过，系统基本可用"
else
    print_error "多个检查失败，请检查日志并修复问题"
fi

echo -e "\n=========================================="
echo "服务访问地址："
echo "=========================================="
echo "  - REST API:      http://localhost:8080"
echo "  - Swagger UI:    http://localhost:8080/swagger-ui.html"
echo "  - Actuator:      http://localhost:8081/actuator"
echo "  - Grafana:       http://localhost:3000 (admin/admin)"
echo "  - Prometheus:    http://localhost:9090"
echo "  - Kafka Connect: http://localhost:8083"
echo "=========================================="

# 提供后续步骤
echo -e "\n下一步："
if [ $failed_checks -gt 0 ]; then
    echo "  1. 查看失败服务的日志: docker-compose logs [服务名]"
    echo "  2. 检查README.md中的故障排查章节"
    echo "  3. 重启失败的服务: docker-compose restart [服务名]"
else
    echo "  1. 浏览API文档: http://localhost:8080/swagger-ui.html"
    echo "  2. 查看监控面板: http://localhost:3000"
    echo "  3. 开始开发Controller层功能"
fi

exit $failed_checks
