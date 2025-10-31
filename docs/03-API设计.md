# API设计文档

## 一、API设计原则

### 1.1 RESTful规范
- 使用标准HTTP方法：GET, POST, PUT, DELETE, PATCH
- 使用复数名词表示资源：`/api/v1/tasks`而非`/api/v1/task`
- 使用HTTP状态码表示操作结果
- 支持资源嵌套：`/api/v1/tasks/{taskId}/mappings`

### 1.2 版本管理
- URL版本控制：`/api/v1/...`, `/api/v2/...`
- 向后兼容，废弃API保留至少2个版本周期
- 在响应头中返回API版本信息

### 1.3 统一响应格式

**成功响应**：
```json
{
  "code": 0,
  "message": "Success",
  "data": { ... },
  "timestamp": "2025-01-30T10:00:00Z",
  "traceId": "uuid"
}
```

**错误响应**：
```json
{
  "code": 40001,
  "message": "Task not found",
  "errors": [
    {
      "field": "taskId",
      "message": "Task with ID xxx does not exist"
    }
  ],
  "timestamp": "2025-01-30T10:00:00Z",
  "traceId": "uuid"
}
```

**分页响应**：
```json
{
  "code": 0,
  "message": "Success",
  "data": {
    "items": [...],
    "pagination": {
      "page": 1,
      "pageSize": 20,
      "total": 100,
      "totalPages": 5
    }
  },
  "timestamp": "2025-01-30T10:00:00Z"
}
```

## 二、认证和授权

### 2.1 JWT Token认证

**获取Token**：
```
POST /api/v1/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "password123"
}

Response:
{
  "code": 0,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 3600,
    "tokenType": "Bearer"
  }
}
```

**使用Token**：
```
GET /api/v1/tasks
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### 2.2 权限模型

| 角色 | 权限 |
|------|------|
| SUPER_ADMIN | 所有权限 |
| TENANT_ADMIN | 租户内所有权限 |
| OPERATOR | 启动/停止任务 |
| VIEWER | 只读权限 |

## 三、核心API接口

### 3.1 租户管理API

#### 3.1.1 创建租户

```
POST /api/v1/tenants
Content-Type: application/json
Authorization: Bearer {token}

Request Body:
{
  "tenantName": "Acme Corporation",
  "tenantCode": "acme",
  "description": "Main tenant for Acme Corp",
  "contactName": "John Doe",
  "contactEmail": "john@acme.com",
  "contactPhone": "+1-555-0100",
  "maxConnectors": 20,
  "maxTasksPerConnector": 8,
  "maxThroughputTps": 50000,
  "config": {
    "alertEmail": "alerts@acme.com",
    "timezone": "America/New_York"
  }
}

Response: 201 Created
{
  "code": 0,
  "message": "Tenant created successfully",
  "data": {
    "tenantId": "uuid",
    "tenantName": "Acme Corporation",
    "tenantCode": "acme",
    "status": "ACTIVE",
    "createdAt": "2025-01-30T10:00:00Z"
  }
}
```

#### 3.1.2 获取租户列表

```
GET /api/v1/tenants?page=1&pageSize=20&status=ACTIVE
Authorization: Bearer {token}

Response: 200 OK
{
  "code": 0,
  "data": {
    "items": [
      {
        "tenantId": "uuid",
        "tenantName": "Acme Corporation",
        "tenantCode": "acme",
        "status": "ACTIVE",
        "maxConnectors": 20,
        "currentConnectors": 5,
        "createdAt": "2025-01-30T10:00:00Z"
      }
    ],
    "pagination": {
      "page": 1,
      "pageSize": 20,
      "total": 1,
      "totalPages": 1
    }
  }
}
```

#### 3.1.3 获取租户详情

```
GET /api/v1/tenants/{tenantId}
Authorization: Bearer {token}

Response: 200 OK
{
  "code": 0,
  "data": {
    "tenantId": "uuid",
    "tenantName": "Acme Corporation",
    "tenantCode": "acme",
    "description": "Main tenant for Acme Corp",
    "contactName": "John Doe",
    "contactEmail": "john@acme.com",
    "status": "ACTIVE",
    "maxConnectors": 20,
    "currentConnectors": 5,
    "config": {},
    "createdAt": "2025-01-30T10:00:00Z",
    "updatedAt": "2025-01-30T10:00:00Z"
  }
}
```

#### 3.1.4 更新租户

```
PUT /api/v1/tenants/{tenantId}
Content-Type: application/json
Authorization: Bearer {token}

Request Body:
{
  "tenantName": "Acme Corporation Ltd",
  "maxConnectors": 30,
  "config": {
    "alertEmail": "new-alerts@acme.com"
  }
}

Response: 200 OK
{
  "code": 0,
  "message": "Tenant updated successfully",
  "data": { ... }
}
```

#### 3.1.5 删除租户

```
DELETE /api/v1/tenants/{tenantId}
Authorization: Bearer {token}

Response: 204 No Content
```

### 3.2 同步任务管理API

#### 3.2.1 创建同步任务

```
POST /api/v1/tasks
Content-Type: application/json
Authorization: Bearer {token}

Request Body:
{
  "tenantId": "uuid",
  "taskName": "MySQL to PostgreSQL Sync",
  "taskCode": "mysql-pg-sync-001",
  "description": "Sync customer data from MySQL to PostgreSQL",
  "sourceDbType": "MYSQL",
  "sourceConnectionConfig": {
    "host": "mysql.example.com",
    "port": 3306,
    "database": "customers_db",
    "username": "dbuser",
    "password": "encrypted:abc123",
    "ssl": true,
    "serverTimezone": "UTC"
  },
  "targetDbType": "POSTGRESQL",
  "targetConnectionConfig": {
    "host": "postgres.example.com",
    "port": 5432,
    "database": "customers_dw",
    "username": "pguser",
    "password": "encrypted:xyz789",
    "ssl": true
  },
  "syncMode": "FULL_INCREMENTAL",
  "connectorConfig": {
    "snapshot.mode": "initial",
    "database.include.list": "customers_db",
    "table.include.list": "customers_db.customers,customers_db.orders",
    "snapshot.max.threads": 4,
    "max.batch.size": 2048
  },
  "alertConfig": {
    "lagThreshold": 100000,
    "errorRateThreshold": 0.01,
    "alertEmails": ["admin@example.com"]
  }
}

Response: 201 Created
{
  "code": 0,
  "message": "Task created successfully",
  "data": {
    "taskId": "uuid",
    "taskName": "MySQL to PostgreSQL Sync",
    "taskCode": "mysql-pg-sync-001",
    "status": "CREATED",
    "connectorName": "mysql-pg-sync-001-connector",
    "createdAt": "2025-01-30T10:00:00Z"
  }
}
```

#### 3.2.2 获取任务列表

```
GET /api/v1/tasks?tenantId=uuid&status=RUNNING&page=1&pageSize=20&sortBy=createdAt&sortOrder=desc
Authorization: Bearer {token}

Query Parameters:
- tenantId: 租户ID（可选，管理员可跨租户查询）
- status: 任务状态（可选）
- sourceDbType: 源数据库类型（可选）
- healthStatus: 健康状态（可选）
- page: 页码（默认1）
- pageSize: 每页大小（默认20）
- sortBy: 排序字段（默认createdAt）
- sortOrder: 排序方向（asc/desc，默认desc）

Response: 200 OK
{
  "code": 0,
  "data": {
    "items": [
      {
        "taskId": "uuid",
        "taskName": "MySQL to PostgreSQL Sync",
        "taskCode": "mysql-pg-sync-001",
        "sourceDbType": "MYSQL",
        "targetDbType": "POSTGRESQL",
        "status": "RUNNING",
        "healthStatus": "HEALTHY",
        "totalRecordsSynced": 1500000,
        "lastSyncTime": "2025-01-30T10:05:00Z",
        "createdAt": "2025-01-30T10:00:00Z"
      }
    ],
    "pagination": { ... }
  }
}
```

#### 3.2.3 获取任务详情

```
GET /api/v1/tasks/{taskId}
Authorization: Bearer {token}

Response: 200 OK
{
  "code": 0,
  "data": {
    "taskId": "uuid",
    "tenantId": "uuid",
    "taskName": "MySQL to PostgreSQL Sync",
    "taskCode": "mysql-pg-sync-001",
    "description": "Sync customer data from MySQL to PostgreSQL",
    "sourceDbType": "MYSQL",
    "sourceConnectionConfig": {
      "host": "mysql.example.com",
      "port": 3306,
      "database": "customers_db",
      "username": "dbuser",
      "password": "******",  // 敏感信息脱敏
      "ssl": true
    },
    "targetDbType": "POSTGRESQL",
    "targetConnectionConfig": { ... },
    "syncMode": "FULL_INCREMENTAL",
    "status": "RUNNING",
    "healthStatus": "HEALTHY",
    "connectorName": "mysql-pg-sync-001-connector",
    "connectorConfig": { ... },
    "totalRecordsSynced": 1500000,
    "lastSyncTime": "2025-01-30T10:05:00Z",
    "lastError": null,
    "errorCount": 0,
    "alertConfig": { ... },
    "createdAt": "2025-01-30T10:00:00Z",
    "updatedAt": "2025-01-30T10:05:00Z"
  }
}
```

#### 3.2.4 更新任务配置

```
PATCH /api/v1/tasks/{taskId}
Content-Type: application/json
Authorization: Bearer {token}

Request Body:
{
  "description": "Updated description",
  "alertConfig": {
    "lagThreshold": 150000,
    "alertEmails": ["admin@example.com", "ops@example.com"]
  }
}

Response: 200 OK
{
  "code": 0,
  "message": "Task updated successfully",
  "data": { ... }
}
```

#### 3.2.5 删除任务

```
DELETE /api/v1/tasks/{taskId}?force=false
Authorization: Bearer {token}

Query Parameters:
- force: 是否强制删除（默认false，true时会立即停止任务并删除）

Response: 204 No Content
```

### 3.3 任务控制API

#### 3.3.1 启动任务

```
POST /api/v1/tasks/{taskId}/start
Authorization: Bearer {token}

Request Body (可选):
{
  "resetOffset": false,  // 是否重置offset从头开始
  "snapshotMode": "initial"  // initial, schema_only, never
}

Response: 200 OK
{
  "code": 0,
  "message": "Task started successfully",
  "data": {
    "taskId": "uuid",
    "status": "RUNNING",
    "connectorName": "mysql-pg-sync-001-connector",
    "startedAt": "2025-01-30T10:10:00Z"
  }
}
```

#### 3.3.2 停止任务

```
POST /api/v1/tasks/{taskId}/stop
Authorization: Bearer {token}

Request Body (可选):
{
  "graceful": true,  // 是否优雅停止（等待当前批次完成）
  "timeoutSeconds": 30
}

Response: 200 OK
{
  "code": 0,
  "message": "Task stopped successfully",
  "data": {
    "taskId": "uuid",
    "status": "STOPPED",
    "stoppedAt": "2025-01-30T10:15:00Z"
  }
}
```

#### 3.3.3 暂停任务

```
POST /api/v1/tasks/{taskId}/pause
Authorization: Bearer {token}

Response: 200 OK
{
  "code": 0,
  "message": "Task paused successfully",
  "data": {
    "taskId": "uuid",
    "status": "PAUSED",
    "pausedAt": "2025-01-30T10:20:00Z"
  }
}
```

#### 3.3.4 恢复任务

```
POST /api/v1/tasks/{taskId}/resume
Authorization: Bearer {token}

Response: 200 OK
{
  "code": 0,
  "message": "Task resumed successfully",
  "data": {
    "taskId": "uuid",
    "status": "RUNNING",
    "resumedAt": "2025-01-30T10:25:00Z"
  }
}
```

#### 3.3.5 重启任务

```
POST /api/v1/tasks/{taskId}/restart
Authorization: Bearer {token}

Response: 200 OK
{
  "code": 0,
  "message": "Task restarted successfully",
  "data": {
    "taskId": "uuid",
    "status": "RUNNING",
    "restartedAt": "2025-01-30T10:30:00Z"
  }
}
```

### 3.4 表映射管理API

#### 3.4.1 创建表映射

```
POST /api/v1/tasks/{taskId}/mappings
Content-Type: application/json
Authorization: Bearer {token}

Request Body:
{
  "sourceSchema": "customers_db",
  "sourceTable": "customers",
  "targetSchema": "public",
  "targetTable": "customers",
  "columnMappings": [
    {
      "sourceColumn": "customer_id",
      "targetColumn": "id",
      "sourceType": "BIGINT",
      "targetType": "BIGINT",
      "nullable": false
    },
    {
      "sourceColumn": "customer_name",
      "targetColumn": "name",
      "sourceType": "VARCHAR(255)",
      "targetType": "VARCHAR(255)",
      "nullable": false
    },
    {
      "sourceColumn": "phone_number",
      "targetColumn": "phone",
      "sourceType": "VARCHAR(20)",
      "targetType": "VARCHAR(20)",
      "nullable": true
    }
  ],
  "primaryKeyMapping": {
    "sourceColumns": ["customer_id"],
    "targetColumns": ["id"]
  },
  "rowFilterCondition": "status = 'ACTIVE'",
  "transformRules": [
    {
      "column": "phone",
      "transformType": "MASK",
      "params": {
        "pattern": "***-****-####"
      }
    }
  ],
  "conflictResolution": "UPSERT",
  "batchSize": 1000
}

Response: 201 Created
{
  "code": 0,
  "message": "Table mapping created successfully",
  "data": {
    "mappingId": "uuid",
    "sourceTableFqn": "customers_db.customers",
    "targetTableFqn": "public.customers",
    "isEnabled": true,
    "createdAt": "2025-01-30T10:00:00Z"
  }
}
```

#### 3.4.2 获取任务的所有映射

```
GET /api/v1/tasks/{taskId}/mappings
Authorization: Bearer {token}

Response: 200 OK
{
  "code": 0,
  "data": {
    "items": [
      {
        "mappingId": "uuid",
        "sourceSchema": "customers_db",
        "sourceTable": "customers",
        "targetSchema": "public",
        "targetTable": "customers",
        "columnMappings": [...],
        "isEnabled": true,
        "totalRowsSynced": 150000,
        "lastSyncTime": "2025-01-30T10:05:00Z",
        "createdAt": "2025-01-30T10:00:00Z"
      }
    ]
  }
}
```

#### 3.4.3 更新表映射

```
PUT /api/v1/tasks/{taskId}/mappings/{mappingId}
Content-Type: application/json
Authorization: Bearer {token}

Request Body:
{
  "columnMappings": [...],
  "rowFilterCondition": "status IN ('ACTIVE', 'PENDING')",
  "isEnabled": true
}

Response: 200 OK
{
  "code": 0,
  "message": "Table mapping updated successfully",
  "data": { ... }
}
```

#### 3.4.4 删除表映射

```
DELETE /api/v1/tasks/{taskId}/mappings/{mappingId}
Authorization: Bearer {token}

Response: 204 No Content
```

### 3.5 转换脚本管理API

#### 3.5.1 创建转换脚本

```
POST /api/v1/mappings/{mappingId}/scripts
Content-Type: application/json
Authorization: Bearer {token}

Request Body:
{
  "scriptName": "Calculate Full Name",
  "scriptType": "GROOVY",
  "triggerPhase": "BEFORE_TRANSFORM",
  "scriptContent": "def transform(record) {\n    record.fullName = record.firstName + ' ' + record.lastName\n    return record\n}",
  "executionOrder": 1,
  "scriptParams": {
    "defaultValue": "N/A"
  },
  "errorHandling": "LOG_AND_CONTINUE",
  "timeoutMs": 5000,
  "isEnabled": true
}

Response: 201 Created
{
  "code": 0,
  "message": "Transform script created successfully",
  "data": {
    "scriptId": "uuid",
    "scriptName": "Calculate Full Name",
    "scriptType": "GROOVY",
    "isEnabled": true,
    "createdAt": "2025-01-30T10:00:00Z"
  }
}
```

#### 3.5.2 测试转换脚本

```
POST /api/v1/mappings/{mappingId}/scripts/test
Content-Type: application/json
Authorization: Bearer {token}

Request Body:
{
  "scriptContent": "def transform(record) { ... }",
  "testData": {
    "firstName": "John",
    "lastName": "Doe"
  }
}

Response: 200 OK
{
  "code": 0,
  "data": {
    "result": {
      "firstName": "John",
      "lastName": "Doe",
      "fullName": "John Doe"
    },
    "executionTimeMs": 25,
    "success": true,
    "error": null
  }
}
```

### 3.6 监控和指标API

#### 3.6.1 获取任务状态

```
GET /api/v1/tasks/{taskId}/status
Authorization: Bearer {token}

Response: 200 OK
{
  "code": 0,
  "data": {
    "taskId": "uuid",
    "status": "RUNNING",
    "healthStatus": "HEALTHY",
    "connectorStatus": {
      "state": "RUNNING",
      "workerId": "connect-worker-1:8083"
    },
    "currentMetrics": {
      "throughput": {
        "recordsPerSecond": 1500,
        "bytesPerSecond": 524288
      },
      "latency": {
        "p50": 45,
        "p95": 180,
        "p99": 420
      },
      "lag": {
        "consumerLag": 5000,
        "timeLagMs": 2500
      },
      "errorRate": 0.0001
    },
    "lastUpdated": "2025-01-30T10:30:00Z"
  }
}
```

#### 3.6.2 获取历史指标

```
GET /api/v1/tasks/{taskId}/metrics?metricType=THROUGHPUT&timeWindow=1HOUR&from=2025-01-30T09:00:00Z&to=2025-01-30T10:00:00Z
Authorization: Bearer {token}

Query Parameters:
- metricType: 指标类型（THROUGHPUT, LATENCY, LAG, ERROR_RATE）
- timeWindow: 时间窗口（REALTIME, 1MIN, 5MIN, 15MIN, 1HOUR, 1DAY）
- from: 开始时间（ISO 8601格式）
- to: 结束时间（ISO 8601格式）

Response: 200 OK
{
  "code": 0,
  "data": {
    "taskId": "uuid",
    "metricType": "THROUGHPUT",
    "timeWindow": "1HOUR",
    "dataPoints": [
      {
        "timestamp": "2025-01-30T09:00:00Z",
        "value": {
          "recordsPerSecond": 1200,
          "bytesPerSecond": 409600
        }
      },
      {
        "timestamp": "2025-01-30T10:00:00Z",
        "value": {
          "recordsPerSecond": 1500,
          "bytesPerSecond": 524288
        }
      }
    ]
  }
}
```

#### 3.6.3 获取Offset信息

```
GET /api/v1/tasks/{taskId}/offsets
Authorization: Bearer {token}

Response: 200 OK
{
  "code": 0,
  "data": {
    "taskId": "uuid",
    "offsets": [
      {
        "partition": {
          "server": "mysql-server-1",
          "database": "customers_db"
        },
        "offset": {
          "file": "mysql-bin.000003",
          "pos": 154987,
          "row": 1,
          "ts_sec": 1706625600
        },
        "lastUpdated": "2025-01-30T10:30:00Z"
      }
    ]
  }
}
```

### 3.7 告警管理API

#### 3.7.1 获取告警列表

```
GET /api/v1/alerts?taskId=uuid&status=OPEN&alertLevel=P1&page=1&pageSize=20
Authorization: Bearer {token}

Response: 200 OK
{
  "code": 0,
  "data": {
    "items": [
      {
        "alertId": "uuid",
        "taskId": "uuid",
        "alertType": "HIGH_LAG",
        "alertLevel": "P1",
        "alertTitle": "High consumer lag detected",
        "alertMessage": "Consumer lag exceeded threshold: 150000 > 100000",
        "alertDetails": {
          "currentLag": 150000,
          "threshold": 100000,
          "durationSeconds": 300
        },
        "status": "OPEN",
        "triggeredAt": "2025-01-30T10:25:00Z"
      }
    ],
    "pagination": { ... }
  }
}
```

#### 3.7.2 确认告警

```
POST /api/v1/alerts/{alertId}/acknowledge
Content-Type: application/json
Authorization: Bearer {token}

Request Body:
{
  "note": "Investigating the issue"
}

Response: 200 OK
{
  "code": 0,
  "message": "Alert acknowledged successfully",
  "data": {
    "alertId": "uuid",
    "status": "ACKNOWLEDGED",
    "acknowledgedAt": "2025-01-30T10:30:00Z",
    "acknowledgedBy": "admin"
  }
}
```

#### 3.7.3 解决告警

```
POST /api/v1/alerts/{alertId}/resolve
Content-Type: application/json
Authorization: Bearer {token}

Request Body:
{
  "resolutionNote": "Increased Kafka partitions to reduce lag"
}

Response: 200 OK
{
  "code": 0,
  "message": "Alert resolved successfully",
  "data": {
    "alertId": "uuid",
    "status": "RESOLVED",
    "resolvedAt": "2025-01-30T10:35:00Z",
    "resolvedBy": "admin"
  }
}
```

### 3.8 审计日志API

#### 3.8.1 获取审计日志

```
GET /api/v1/audit-logs?operationType=START_TASK&resourceType=TASK&from=2025-01-30T00:00:00Z&to=2025-01-30T23:59:59Z&page=1&pageSize=20
Authorization: Bearer {token}

Response: 200 OK
{
  "code": 0,
  "data": {
    "items": [
      {
        "logId": "uuid",
        "tenantId": "uuid",
        "operationType": "START_TASK",
        "resourceType": "TASK",
        "resourceId": "uuid",
        "operationDetails": {
          "taskName": "MySQL to PostgreSQL Sync",
          "resetOffset": false
        },
        "status": "SUCCESS",
        "operator": "admin",
        "operatorIp": "192.168.1.100",
        "operatedAt": "2025-01-30T10:10:00Z"
      }
    ],
    "pagination": { ... }
  }
}
```

## 四、错误码定义

### 4.1 HTTP状态码

| 状态码 | 说明 |
|--------|------|
| 200 | 成功 |
| 201 | 创建成功 |
| 204 | 删除成功（无内容）|
| 400 | 请求参数错误 |
| 401 | 未认证 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 409 | 资源冲突 |
| 422 | 请求参数验证失败 |
| 429 | 请求过于频繁 |
| 500 | 服务器内部错误 |
| 503 | 服务不可用 |

### 4.2 业务错误码

| 错误码 | 说明 |
|--------|------|
| 0 | 成功 |
| 10001 | 参数错误 |
| 10002 | 参数验证失败 |
| 20001 | 认证失败 |
| 20002 | Token过期 |
| 20003 | 无权限 |
| 30001 | 租户不存在 |
| 30002 | 租户已存在 |
| 30003 | 租户配额不足 |
| 40001 | 任务不存在 |
| 40002 | 任务已存在 |
| 40003 | 任务状态错误 |
| 40004 | Connector创建失败 |
| 50001 | 表映射不存在 |
| 50002 | 表映射已存在 |
| 60001 | 脚本执行失败 |
| 60002 | 脚本语法错误 |
| 70001 | 数据库连接失败 |
| 70002 | 数据库操作失败 |
| 90001 | 系统内部错误 |
| 90002 | 服务不可用 |

## 五、API限流策略

### 5.1 限流规则

| 端点类别 | 限流规则 |
|---------|---------|
| 认证接口 | 10次/分钟/IP |
| 查询接口 | 100次/分钟/用户 |
| 写入接口 | 50次/分钟/用户 |
| 控制接口 | 20次/分钟/用户 |

### 5.2 限流响应

```
Response: 429 Too Many Requests
{
  "code": 42901,
  "message": "Too many requests",
  "errors": [{
    "message": "Rate limit exceeded. Please try again in 30 seconds."
  }],
  "retryAfter": 30
}

Headers:
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 0
X-RateLimit-Reset: 1706625660
Retry-After: 30
```

## 六、Webhook回调

### 6.1 任务状态变更回调

```
POST {webhook_url}
Content-Type: application/json
X-Webhook-Signature: sha256=...

Request Body:
{
  "eventType": "TASK_STATUS_CHANGED",
  "eventId": "uuid",
  "timestamp": "2025-01-30T10:10:00Z",
  "data": {
    "taskId": "uuid",
    "taskName": "MySQL to PostgreSQL Sync",
    "previousStatus": "CREATED",
    "currentStatus": "RUNNING",
    "triggeredBy": "admin"
  }
}
```

### 6.2 告警回调

```
POST {webhook_url}
Content-Type: application/json
X-Webhook-Signature: sha256=...

Request Body:
{
  "eventType": "ALERT_TRIGGERED",
  "eventId": "uuid",
  "timestamp": "2025-01-30T10:25:00Z",
  "data": {
    "alertId": "uuid",
    "taskId": "uuid",
    "alertType": "HIGH_LAG",
    "alertLevel": "P1",
    "alertMessage": "Consumer lag exceeded threshold",
    "alertDetails": { ... }
  }
}
```

---

**文档版本**：v1.0
**最后更新**：2025-01-30
