package com.dbsync.core.repository;

import com.dbsync.common.enums.TaskStatus;
import com.dbsync.core.domain.entity.SyncTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Sync Task Repository
 *
 * @author DB Sync Platform
 */
@Repository
public interface SyncTaskRepository extends JpaRepository<SyncTask, UUID> {

    /**
     * Find by tenant ID
     */
    Page<SyncTask> findByTenantIdAndDeletedAtIsNull(UUID tenantId, Pageable pageable);

    /**
     * Find by tenant ID and status
     */
    Page<SyncTask> findByTenantIdAndStatusAndDeletedAtIsNull(UUID tenantId, TaskStatus status, Pageable pageable);

    /**
     * Find by status
     */
    List<SyncTask> findByStatusAndDeletedAtIsNull(TaskStatus status);

    /**
     * Find by task code
     */
    Optional<SyncTask> findByTenantIdAndTaskCodeAndDeletedAtIsNull(UUID tenantId, String taskCode);

    /**
     * Find by connector name
     */
    Optional<SyncTask> findByConnectorName(String connectorName);

    /**
     * Check if task code exists for tenant
     */
    boolean existsByTenantIdAndTaskCode(UUID tenantId, String taskCode);

    /**
     * Count tasks by tenant
     */
    @Query("SELECT COUNT(t) FROM SyncTask t WHERE t.tenantId = :tenantId AND t.deletedAt IS NULL")
    long countByTenantId(@Param("tenantId") UUID tenantId);

    /**
     * Count running tasks by tenant
     */
    @Query("SELECT COUNT(t) FROM SyncTask t WHERE t.tenantId = :tenantId AND t.status = :status AND t.deletedAt IS NULL")
    long countByTenantIdAndStatus(@Param("tenantId") UUID tenantId, @Param("status") TaskStatus status);
}
