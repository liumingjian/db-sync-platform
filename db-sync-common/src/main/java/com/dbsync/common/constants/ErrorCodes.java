package com.dbsync.common.constants;

/**
 * Error Code Constants
 *
 * @author DB Sync Platform
 */
public final class ErrorCodes {

    private ErrorCodes() {
        throw new UnsupportedOperationException("This is a constants class and cannot be instantiated");
    }

    // Success
    public static final int SUCCESS = 0;

    // Parameter Errors (10xxx)
    public static final int PARAM_ERROR = 10001;
    public static final int PARAM_VALIDATION_FAILED = 10002;

    // Authentication & Authorization (20xxx)
    public static final int AUTH_FAILED = 20001;
    public static final int TOKEN_EXPIRED = 20002;
    public static final int NO_PERMISSION = 20003;

    // Tenant Errors (30xxx)
    public static final int TENANT_NOT_FOUND = 30001;
    public static final int TENANT_ALREADY_EXISTS = 30002;
    public static final int TENANT_QUOTA_EXCEEDED = 30003;

    // Task Errors (40xxx)
    public static final int TASK_NOT_FOUND = 40001;
    public static final int TASK_ALREADY_EXISTS = 40002;
    public static final int TASK_STATUS_ERROR = 40003;
    public static final int CONNECTOR_CREATION_FAILED = 40004;

    // Mapping Errors (50xxx)
    public static final int MAPPING_NOT_FOUND = 50001;
    public static final int MAPPING_ALREADY_EXISTS = 50002;

    // Script Errors (60xxx)
    public static final int SCRIPT_EXECUTION_FAILED = 60001;
    public static final int SCRIPT_SYNTAX_ERROR = 60002;

    // Database Errors (70xxx)
    public static final int DB_CONNECTION_FAILED = 70001;
    public static final int DB_OPERATION_FAILED = 70002;

    // System Errors (90xxx)
    public static final int SYSTEM_ERROR = 90001;
    public static final int SERVICE_UNAVAILABLE = 90002;
}
