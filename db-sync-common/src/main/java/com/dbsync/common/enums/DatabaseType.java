package com.dbsync.common.enums;

import lombok.Getter;

/**
 * Database Type Enum
 *
 * @author DB Sync Platform
 */
@Getter
public enum DatabaseType {

    MYSQL("MySQL", "com.mysql.cj.jdbc.Driver", "jdbc:mysql://"),
    ORACLE("Oracle", "oracle.jdbc.OracleDriver", "jdbc:oracle:thin:@"),
    SQLSERVER("SQL Server", "com.microsoft.sqlserver.jdbc.SQLServerDriver", "jdbc:sqlserver://"),
    POSTGRESQL("PostgreSQL", "org.postgresql.Driver", "jdbc:postgresql://");

    private final String displayName;
    private final String driverClassName;
    private final String urlPrefix;

    DatabaseType(String displayName, String driverClassName, String urlPrefix) {
        this.displayName = displayName;
        this.driverClassName = driverClassName;
        this.urlPrefix = urlPrefix;
    }

    public static DatabaseType fromString(String type) {
        for (DatabaseType dbType : values()) {
            if (dbType.name().equalsIgnoreCase(type)) {
                return dbType;
            }
        }
        throw new IllegalArgumentException("Unknown database type: " + type);
    }
}
