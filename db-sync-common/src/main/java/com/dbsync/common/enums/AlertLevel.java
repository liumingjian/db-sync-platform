package com.dbsync.common.enums;

import lombok.Getter;

/**
 * Alert Level Enum
 *
 * @author DB Sync Platform
 */
@Getter
public enum AlertLevel {

    P0("P0", "Critical - Service outage", 1),
    P1("P1", "Severe - Data sync affected", 2),
    P2("P2", "Important - Performance issue", 3),
    P3("P3", "Normal - Notification only", 4);

    private final String level;
    private final String description;
    private final int priority;

    AlertLevel(String level, String description, int priority) {
        this.level = level;
        this.description = description;
        this.priority = priority;
    }
}
