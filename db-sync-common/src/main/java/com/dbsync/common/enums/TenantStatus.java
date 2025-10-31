package com.dbsync.common.enums;

import lombok.Getter;

/**
 * Tenant Status Enum
 *
 * @author DB Sync Platform
 */
@Getter
public enum TenantStatus {

    ACTIVE("Active", "Tenant is active"),
    SUSPENDED("Suspended", "Tenant is suspended"),
    INACTIVE("Inactive", "Tenant is inactive");

    private final String displayName;
    private final String description;

    TenantStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
}
