package com.dbsync.common.enums;

import lombok.Getter;

/**
 * Health Status Enum
 *
 * @author DB Sync Platform
 */
@Getter
public enum HealthStatus {

    HEALTHY("Healthy", "System is operating normally"),
    DEGRADED("Degraded", "System is operating with minor issues"),
    UNHEALTHY("Unhealthy", "System has serious issues"),
    PAUSED("Paused", "System is paused"),
    UNKNOWN("Unknown", "Health status cannot be determined");

    private final String displayName;
    private final String description;

    HealthStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
}
