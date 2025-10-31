package com.dbsync.core.repository;

import com.dbsync.common.enums.TenantStatus;
import com.dbsync.core.domain.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Tenant Repository
 *
 * @author DB Sync Platform
 */
@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    /**
     * Find by tenant code
     */
    Optional<Tenant> findByTenantCode(String tenantCode);

    /**
     * Find by status
     */
    List<Tenant> findByStatus(TenantStatus status);

    /**
     * Find by status and deletedAt is null
     */
    List<Tenant> findByStatusAndDeletedAtIsNull(TenantStatus status);

    /**
     * Check if tenant code exists
     */
    boolean existsByTenantCode(String tenantCode);
}
