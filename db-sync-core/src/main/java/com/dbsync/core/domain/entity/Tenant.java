package com.dbsync.core.domain.entity;

import com.dbsync.common.enums.TenantStatus;
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
 * Tenant Entity
 *
 * @author DB Sync Platform
 */
@Data
@Entity
@Table(name = "tenants", indexes = {
        @Index(name = "idx_tenants_status", columnList = "status"),
        @Index(name = "idx_tenants_code", columnList = "tenant_code")
})
@EntityListeners(AuditingEntityListener.class)
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "tenant_name", nullable = false, length = 100)
    private String tenantName;

    @Column(name = "tenant_code", nullable = false, unique = true, length = 50)
    private String tenantCode;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "contact_name", length = 100)
    private String contactName;

    @Column(name = "contact_email", length = 255)
    private String contactEmail;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Column(name = "max_connectors")
    private Integer maxConnectors = 10;

    @Column(name = "max_tasks_per_connector")
    private Integer maxTasksPerConnector = 8;

    @Column(name = "max_throughput_tps")
    private Integer maxThroughputTps = 10000;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TenantStatus status = TenantStatus.ACTIVE;

    @Column(name = "config", columnDefinition = "jsonb")
    private String config;  // Store as JSON string

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
}
