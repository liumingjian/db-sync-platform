package com.dbsync.core.domain.entity;

import com.dbsync.common.enums.DatabaseType;
import com.dbsync.common.enums.HealthStatus;
import com.dbsync.common.enums.SyncMode;
import com.dbsync.common.enums.TaskStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Sync Task Entity
 *
 * @author DB Sync Platform
 */
@Data
@Entity
@Table(name = "sync_tasks", indexes = {
        @Index(name = "idx_sync_tasks_tenant", columnList = "tenant_id"),
        @Index(name = "idx_sync_tasks_status", columnList = "status"),
        @Index(name = "idx_sync_tasks_health", columnList = "health_status"),
        @Index(name = "idx_sync_tasks_connector", columnList = "connector_name")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_tenant_task_code", columnNames = {"tenant_id", "task_code"})
})
@EntityListeners(AuditingEntityListener.class)
public class SyncTask {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "task_id")
    private UUID taskId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "task_name", nullable = false, length = 200)
    private String taskName;

    @Column(name = "task_code", nullable = false, length = 100)
    private String taskCode;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_db_type", nullable = false, length = 50)
    private DatabaseType sourceDbType;

    @Column(name = "source_connection_config", nullable = false, columnDefinition = "jsonb")
    private String sourceConnectionConfig;  // JSON string

    @Enumerated(EnumType.STRING)
    @Column(name = "target_db_type", nullable = false, length = 50)
    private DatabaseType targetDbType;

    @Column(name = "target_connection_config", nullable = false, columnDefinition = "jsonb")
    private String targetConnectionConfig;  // JSON string

    @Column(name = "connector_name", unique = true, length = 200)
    private String connectorName;

    @Column(name = "connector_config", nullable = false, columnDefinition = "jsonb")
    private String connectorConfig;  // JSON string

    @Enumerated(EnumType.STRING)
    @Column(name = "sync_mode", nullable = false, length = 20)
    private SyncMode syncMode = SyncMode.FULL_INCREMENTAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TaskStatus status = TaskStatus.CREATED;

    @Enumerated(EnumType.STRING)
    @Column(name = "health_status", length = 20)
    private HealthStatus healthStatus = HealthStatus.UNKNOWN;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "error_count")
    private Integer errorCount = 0;

    @Column(name = "total_records_synced")
    private Long totalRecordsSynced = 0L;

    @Column(name = "last_sync_time")
    private LocalDateTime lastSyncTime;

    @Column(name = "alert_config", columnDefinition = "jsonb")
    private String alertConfig;  // JSON string

    @Column(name = "schedule_config", columnDefinition = "jsonb")
    private String scheduleConfig;  // JSON string

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @CreatedBy
    @Column(name = "created_by", length = 100)
    private String createdBy;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @LastModifiedBy
    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * Check if task can transition to target status
     */
    public boolean canTransitionTo(TaskStatus targetStatus) {
        return this.status.canTransitionTo(targetStatus);
    }
}
