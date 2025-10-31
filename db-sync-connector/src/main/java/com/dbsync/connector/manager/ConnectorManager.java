package com.dbsync.connector.manager;

import com.dbsync.common.enums.DatabaseType;
import com.dbsync.common.enums.HealthStatus;
import com.dbsync.common.exceptions.BusinessException;
import com.dbsync.connector.builder.ConnectorBuilder;
import com.dbsync.connector.builder.MySQLConnectorBuilder;
import com.dbsync.connector.client.KafkaConnectClient;
import com.dbsync.core.domain.entity.SyncTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Connector lifecycle manager
 * Manages creation, start, stop, and deletion of Kafka Connect connectors
 *
 * @author DB Sync Platform
 */
@Slf4j
@Component
public class ConnectorManager {

    private final KafkaConnectClient connectClient;
    private final Map<DatabaseType, ConnectorBuilder> connectorBuilders;

    public ConnectorManager(
            KafkaConnectClient connectClient,
            MySQLConnectorBuilder mysqlConnectorBuilder) {
        this.connectClient = connectClient;

        // Register connector builders
        this.connectorBuilders = new HashMap<>();
        this.connectorBuilders.put(DatabaseType.MYSQL, mysqlConnectorBuilder);
        // Additional builders will be added here for Oracle, SQL Server, etc.
    }

    /**
     * Create and start a connector for the given sync task
     */
    public String createConnector(SyncTask task) {
        log.info("Creating connector for task: {}", task.getTaskCode());

        try {
            // Get the appropriate connector builder
            ConnectorBuilder builder = getConnectorBuilder(task.getSourceDbType());

            // Validate source database connection
            if (!builder.validateConnection(task.getSourceConnectionConfig())) {
                throw new BusinessException("Failed to validate source database connection");
            }

            // Build connector configuration
            Map<String, String> config = builder.buildConfig(task);

            // Generate unique connector name
            String connectorName = generateConnectorName(task);

            // Create connector via Kafka Connect REST API
            KafkaConnectClient.ConnectorInfo connectorInfo =
                    connectClient.createConnector(connectorName, config);

            log.info("Connector created successfully: {}", connectorName);
            return connectorName;

        } catch (Exception e) {
            log.error("Failed to create connector for task {}: {}",
                    task.getTaskCode(), e.getMessage(), e);
            throw new BusinessException("Failed to create connector: " + e.getMessage());
        }
    }

    /**
     * Get connector status
     */
    public ConnectorHealthInfo getConnectorHealth(String connectorName) {
        log.debug("Getting health status for connector: {}", connectorName);

        try {
            KafkaConnectClient.ConnectorStatus status = connectClient.getConnectorStatus(connectorName);

            if (status == null) {
                return new ConnectorHealthInfo(HealthStatus.UNKNOWN, "Connector not found", null);
            }

            // Determine health status based on connector state
            HealthStatus healthStatus = determineHealthStatus(status);
            String message = buildHealthMessage(status);

            return new ConnectorHealthInfo(healthStatus, message, status);

        } catch (Exception e) {
            log.error("Failed to get connector health: {}", e.getMessage());
            return new ConnectorHealthInfo(HealthStatus.UNKNOWN, "Failed to get status: " + e.getMessage(), null);
        }
    }

    /**
     * Stop connector (pause)
     */
    public void stopConnector(String connectorName) {
        log.info("Stopping connector: {}", connectorName);

        try {
            connectClient.pauseConnector(connectorName);
            log.info("Connector stopped successfully: {}", connectorName);

        } catch (Exception e) {
            log.error("Failed to stop connector {}: {}", connectorName, e.getMessage(), e);
            throw new BusinessException("Failed to stop connector: " + e.getMessage());
        }
    }

    /**
     * Start connector (resume)
     */
    public void startConnector(String connectorName) {
        log.info("Starting connector: {}", connectorName);

        try {
            // Check if connector exists
            KafkaConnectClient.ConnectorInfo info = connectClient.getConnectorInfo(connectorName);

            if (info == null) {
                throw new BusinessException("Connector not found: " + connectorName);
            }

            // Resume the connector
            connectClient.resumeConnector(connectorName);
            log.info("Connector started successfully: {}", connectorName);

        } catch (Exception e) {
            log.error("Failed to start connector {}: {}", connectorName, e.getMessage(), e);
            throw new BusinessException("Failed to start connector: " + e.getMessage());
        }
    }

    /**
     * Restart connector
     */
    public void restartConnector(String connectorName) {
        log.info("Restarting connector: {}", connectorName);

        try {
            connectClient.restartConnector(connectorName);
            log.info("Connector restarted successfully: {}", connectorName);

        } catch (Exception e) {
            log.error("Failed to restart connector {}: {}", connectorName, e.getMessage(), e);
            throw new BusinessException("Failed to restart connector: " + e.getMessage());
        }
    }

    /**
     * Delete connector
     */
    public void deleteConnector(String connectorName) {
        log.info("Deleting connector: {}", connectorName);

        try {
            connectClient.deleteConnector(connectorName);
            log.info("Connector deleted successfully: {}", connectorName);

        } catch (Exception e) {
            log.error("Failed to delete connector {}: {}", connectorName, e.getMessage(), e);
            throw new BusinessException("Failed to delete connector: " + e.getMessage());
        }
    }

    /**
     * Update connector configuration
     */
    public void updateConnectorConfig(SyncTask task) {
        log.info("Updating connector config for task: {}", task.getTaskCode());

        try {
            String connectorName = task.getConnectorName();

            if (connectorName == null) {
                throw new BusinessException("Connector name is null for task: " + task.getTaskCode());
            }

            // Get the appropriate connector builder
            ConnectorBuilder builder = getConnectorBuilder(task.getSourceDbType());

            // Build new connector configuration
            Map<String, String> config = builder.buildConfig(task);

            // Update connector via Kafka Connect REST API
            connectClient.updateConnectorConfig(connectorName, config);

            log.info("Connector config updated successfully: {}", connectorName);

        } catch (Exception e) {
            log.error("Failed to update connector config for task {}: {}",
                    task.getTaskCode(), e.getMessage(), e);
            throw new BusinessException("Failed to update connector config: " + e.getMessage());
        }
    }

    /**
     * Check if connector exists
     */
    public boolean connectorExists(String connectorName) {
        try {
            KafkaConnectClient.ConnectorInfo info = connectClient.getConnectorInfo(connectorName);
            return info != null;
        } catch (Exception e) {
            log.error("Failed to check if connector exists: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get connector builder for database type
     */
    private ConnectorBuilder getConnectorBuilder(DatabaseType dbType) {
        ConnectorBuilder builder = connectorBuilders.get(dbType);

        if (builder == null) {
            throw new BusinessException("Unsupported database type: " + dbType);
        }

        return builder;
    }

    /**
     * Generate unique connector name
     */
    private String generateConnectorName(SyncTask task) {
        return task.getTaskCode() + "-connector";
    }

    /**
     * Determine health status from connector status
     */
    private HealthStatus determineHealthStatus(KafkaConnectClient.ConnectorStatus status) {
        String state = status.getState();

        if ("RUNNING".equalsIgnoreCase(state)) {
            // Check if all tasks are running
            boolean allTasksHealthy = status.getTasks().stream()
                    .allMatch(task -> "RUNNING".equalsIgnoreCase(task.getState()));

            if (allTasksHealthy) {
                return HealthStatus.HEALTHY;
            } else {
                return HealthStatus.DEGRADED;
            }
        } else if ("PAUSED".equalsIgnoreCase(state)) {
            return HealthStatus.PAUSED;
        } else if ("FAILED".equalsIgnoreCase(state)) {
            return HealthStatus.UNHEALTHY;
        } else {
            return HealthStatus.UNKNOWN;
        }
    }

    /**
     * Build health message from connector status
     */
    private String buildHealthMessage(KafkaConnectClient.ConnectorStatus status) {
        StringBuilder message = new StringBuilder();
        message.append("Connector state: ").append(status.getState());

        if (status.getTasks() != null && !status.getTasks().isEmpty()) {
            long runningTasks = status.getTasks().stream()
                    .filter(task -> "RUNNING".equalsIgnoreCase(task.getState()))
                    .count();

            message.append(", Running tasks: ")
                    .append(runningTasks)
                    .append("/")
                    .append(status.getTasks().size());

            // Add failed task traces if any
            status.getTasks().stream()
                    .filter(task -> "FAILED".equalsIgnoreCase(task.getState()))
                    .findFirst()
                    .ifPresent(task -> message.append(", Error: ").append(task.getTrace()));
        }

        return message.toString();
    }

    /**
     * Connector health information
     */
    public static class ConnectorHealthInfo {
        private final HealthStatus healthStatus;
        private final String message;
        private final KafkaConnectClient.ConnectorStatus connectorStatus;

        public ConnectorHealthInfo(
                HealthStatus healthStatus,
                String message,
                KafkaConnectClient.ConnectorStatus connectorStatus) {
            this.healthStatus = healthStatus;
            this.message = message;
            this.connectorStatus = connectorStatus;
        }

        public HealthStatus getHealthStatus() {
            return healthStatus;
        }

        public String getMessage() {
            return message;
        }

        public KafkaConnectClient.ConnectorStatus getConnectorStatus() {
            return connectorStatus;
        }
    }
}
