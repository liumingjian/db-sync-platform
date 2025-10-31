# Swagger UI 访问说明

## 🎉 好消息: 无需登录!

在开发环境下,Swagger UI 是**完全开放**的,不需要任何用户名和密码!

## 快速访问

1. **启动应用**
   ```bash
   mvn spring-boot:run -pl db-sync-api
   ```

2. **直接访问**

   浏览器打开: http://localhost:8080/swagger-ui.html

   就这么简单! 🚀

## 访问地址

| 服务 | URL | 说明 |
|------|-----|------|
| **Swagger UI** | http://localhost:8080/swagger-ui.html | API 文档界面 ✅ 无需认证 |
| **OpenAPI JSON** | http://localhost:8080/api-docs | OpenAPI 规范 JSON |
| **健康检查** | http://localhost:8080/actuator/health | 应用健康状态 |
| **监控指标** | http://localhost:8081/actuator/prometheus | Prometheus 指标 |

## 为什么不需要登录?

在开发环境 (profile=dev) 下,我们配置了安全策略允许所有请求:

```java
@Bean
@Profile("dev")
public SecurityFilterChain devSecurityFilterChain(HttpSecurity http) {
    http.authorizeHttpRequests(auth -> auth
        .anyRequest().permitAll()  // 所有请求都允许通过
    );
    return http.build();
}
```

这样做的好处:
- ✅ 方便本地开发和调试
- ✅ 可以直接测试 API
- ✅ 无需每次都获取 Token
- ✅ 加快开发速度

## 生产环境

⚠️ **注意**: 生产环境会启用 JWT 认证!

切换到生产环境后:
1. 需要先调用登录接口获取 Token
2. 在 Swagger UI 中点击 "Authorize" 按钮
3. 输入 Token: `Bearer <your-token>`
4. 然后才能测试需要认证的接口

## 当前可用的 API

虽然还没有实现业务 API,但可以访问:

### 健康检查接口
```bash
curl http://localhost:8080/actuator/health
```

响应:
```json
{
  "status": "UP",
  "groups": ["liveness", "readiness"]
}
```

### Actuator 端点
- `/actuator/info` - 应用信息
- `/actuator/health` - 健康检查
- `/actuator/metrics` - 性能指标
- `/actuator/prometheus` - Prometheus 指标

## 下一步

Week 5-6 会实现:
1. 租户管理 API
2. 同步任务管理 API
3. JWT 认证和授权
4. 用户管理功能

届时 Swagger UI 上就会显示完整的 API 列表!

## 问题排查

### 无法访问 Swagger UI?

1. **检查应用是否启动**
   ```bash
   curl http://localhost:8080/actuator/health
   ```
   应该返回 `{"status":"UP"}`

2. **检查端口占用**
   ```bash
   lsof -i :8080
   ```

3. **查看应用日志**
   ```bash
   tail -f logs/db-sync-platform.log
   ```

4. **确认 Profile**
   启动日志应该显示:
   ```
   The following 1 profile is active: "dev"
   ```

### 看到 401 Unauthorized?

说明不是使用 `dev` profile,检查:
```bash
# 确保使用 dev profile
mvn spring-boot:run -pl db-sync-api -Dspring-boot.run.profiles=dev
```

## 相关文档

- [QUICK_START.md](QUICK_START.md) - 快速启动指南
- [SECURITY_NOTE.md](SECURITY_NOTE.md) - 详细的安全配置说明
- [FIXES.md](FIXES.md) - 问题修复记录

---

**总结**: 开发环境下直接访问 http://localhost:8080/swagger-ui.html 即可,无需任何认证! 🎊
