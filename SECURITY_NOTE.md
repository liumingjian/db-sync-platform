# 安全配置说明

## 当前状态

### 开发环境 (dev profile)
**无需登录!** 🎉

在开发环境下,所有 API 端点和 Swagger UI 都是**完全开放**的,无需任何认证。

#### 访问方式
直接访问以下 URL,无需登录:
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **API 文档**: http://localhost:8080/api-docs
- **健康检查**: http://localhost:8080/actuator/health
- **所有 API 端点**: http://localhost:8080/api/v1/**

### 生产环境 (非 dev profile)
启用 JWT 认证,需要:
1. 先调用登录接口获取 Token
2. 在请求头中携带 `Authorization: Bearer <token>`

## 如何切换环境

### 使用开发环境(推荐)
```bash
# 方式 1: 通过环境变量
export SPRING_PROFILES_ACTIVE=dev
mvn spring-boot:run -pl db-sync-api

# 方式 2: 通过 Maven 参数
mvn spring-boot:run -pl db-sync-api -Dspring-boot.run.profiles=dev

# 方式 3: 在 IDEA 中设置
# Run -> Edit Configurations -> Active profiles: dev
```

### 使用生产环境
```bash
# 方式 1: 通过环境变量
export SPRING_PROFILES_ACTIVE=prod
mvn spring-boot:run -pl db-sync-api

# 方式 2: 通过 Maven 参数
mvn spring-boot:run -pl db-sync-api -Dspring-boot.run.profiles=prod
```

## 安全配置详情

### SecurityConfig 类
**位置**: `db-sync-api/src/main/java/com/dbsync/api/config/SecurityConfig.java`

#### 开发环境配置
```java
@Bean
@Profile("dev")
public SecurityFilterChain devSecurityFilterChain(HttpSecurity http) {
    http
        .csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(auth -> auth
            .anyRequest().permitAll()  // 所有请求都允许
        );
    return http.build();
}
```

#### 生产环境配置
```java
@Bean
@Profile("!dev")
public SecurityFilterChain prodSecurityFilterChain(HttpSecurity http) {
    http
        .csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(auth -> auth
            // 公开端点
            .requestMatchers(
                "/api/v1/auth/**",
                "/swagger-ui/**",
                "/api-docs/**",
                "/actuator/health",
                "/actuator/info"
            ).permitAll()
            // 其他端点需要认证
            .anyRequest().authenticated()
        );
    return http.build();
}
```

## 验证当前配置

### 检查当前 Profile
启动应用后,查看日志:
```
The following 1 profile is active: "dev"
```

### 测试无认证访问
```bash
# 访问 Swagger UI
curl http://localhost:8080/swagger-ui.html

# 访问健康检查
curl http://localhost:8080/actuator/health

# 应该都能正常访问,不会返回 401 Unauthorized
```

## 下一步开发 (Week 5-6)

根据实施路线图,接下来会实现:

### 1. JWT 认证
- [ ] 登录接口 (`POST /api/v1/auth/login`)
- [ ] Token 刷新 (`POST /api/v1/auth/refresh`)
- [ ] 登出接口 (`POST /api/v1/auth/logout`)

### 2. 用户管理
- [ ] 用户实体和 Repository
- [ ] 用户注册和管理
- [ ] 密码加密 (BCrypt)

### 3. 权限控制
- [ ] 基于角色的访问控制 (RBAC)
- [ ] 角色: SUPER_ADMIN, TENANT_ADMIN, OPERATOR, VIEWER
- [ ] 方法级权限控制 (`@PreAuthorize`)

### 4. JWT Token 配置
```yaml
app:
  security:
    jwt:
      secret: ${JWT_SECRET:your-secret-key-change-in-production}
      expiration: 3600000  # 1 hour
      refresh-expiration: 604800000  # 7 days
```

## 安全提示

⚠️ **重要**: 开发环境配置仅用于本地开发和测试!

- ✅ **开发环境**: 可以禁用认证,方便开发调试
- ❌ **生产环境**: 必须启用完整的认证和授权机制
- 🔐 **JWT Secret**: 生产环境必须使用强密钥
- 🚫 **CSRF**: 生产环境建议启用 CSRF 保护
- 🔒 **HTTPS**: 生产环境必须使用 HTTPS

## 常见问题

### Q: 为什么访问 Swagger UI 不需要登录?
A: 因为当前使用的是 `dev` profile,安全配置允许所有请求通过。

### Q: 如何启用认证?
A: 切换到非 dev profile (如 `prod`),并实现 JWT 认证功能。

### Q: 生产环境如何配置?
A:
1. 设置环境变量 `SPRING_PROFILES_ACTIVE=prod`
2. 配置 JWT Secret: `JWT_SECRET=your-strong-secret-key`
3. 实现登录接口和 JWT 过滤器 (Week 5-6)

---

**创建时间**: 2025-10-31
**适用版本**: Phase 1 Week 3-4
**状态**: ✅ 开发环境认证已禁用,Swagger UI 可直接访问
