package com.dbsync.common.enums;

import lombok.Getter;

/**
 * Sync Mode Enum
 *
 * @author DB Sync Platform
 */
@Getter
public enum SyncMode {

    FULL_ONLY("Full Sync Only", "Only perform full table sync"),
    INCREMENTAL_ONLY("Incremental Sync Only", "Only perform incremental sync"),
    FULL_INCREMENTAL("Full + Incremental", "Perform full sync first, then incremental sync");

    private final String displayName;
    private final String description;

    SyncMode(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
}
