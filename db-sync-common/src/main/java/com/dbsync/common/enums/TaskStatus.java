package com.dbsync.common.enums;

import lombok.Getter;

/**
 * Task Status Enum
 *
 * @author DB Sync Platform
 */
@Getter
public enum TaskStatus {

    CREATED("Created", "Task has been created but not started"),
    RUNNING("Running", "Task is currently running"),
    PAUSED("Paused", "Task has been paused"),
    STOPPED("Stopped", "Task has been stopped"),
    FAILED("Failed", "Task has failed"),
    COMPLETED("Completed", "Task has completed (full sync only)");

    private final String displayName;
    private final String description;

    TaskStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public boolean canTransitionTo(TaskStatus target) {
        return switch (this) {
            case CREATED -> target == RUNNING;
            case RUNNING -> target == PAUSED || target == STOPPED || target == FAILED || target == COMPLETED;
            case PAUSED -> target == RUNNING || target == STOPPED;
            case STOPPED -> target == RUNNING;
            case FAILED -> target == RUNNING;
            case COMPLETED -> target == RUNNING;
        };
    }
}
