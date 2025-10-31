package com.dbsync.connector.builder;

import com.dbsync.common.exceptions.BusinessException;
import com.dbsync.common.utils.JsonUtil;
import com.dbsync.core.domain.entity.SyncTask;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * MySQL Debezium Connector configuration builder
 *
 * @author DB Sync Platform
 */
@Slf4j
@Component
public class MySQLConnectorBuilder implements ConnectorBuilder {

    private static final String CONNECTOR_CLASS = "io.debezium.connector.mysql.MySqlConnector";

    @Override
    public Map<String, String> buildConfig(SyncTask task) {
        log.info("Building MySQL connector config for task: {}", task.getTaskCode());

        try {
            JsonNode sourceConfig = JsonUtil.parseJson(task.getSourceConnectionConfig());
            JsonNode connectorConfig = JsonUtil.parseJson(task.getConnectorConfig());

            Map<String, String> config = new HashMap<>();

            // Basic connector configuration
            config.put("connector.class", CONNECTOR_CLASS);
            config.put("tasks.max", "1");

            // Database connection
            config.put("database.hostname", sourceConfig.get("host").asText());
            config.put("database.port", sourceConfig.get("port").asText("3306"));
            config.put("database.user", sourceConfig.get("username").asText());
            config.put("database.password", sourceConfig.get("password").asText());

            // Database selection
            if (sourceConfig.has("database")) {
                config.put("database.include.list", sourceConfig.get("database").asText());
            }

            // Server identification (must be unique across all connectors)
            String serverId = task.getTenantId().toString().replace("-", "").substring(0, 10);
            config.put("database.server.id", serverId);
            config.put("database.server.name", task.getTaskCode().replace("-", "_"));

            // Topic prefix
            config.put("topic.prefix", task.getTenantId().toString());

            // Snapshot mode
            String snapshotMode = getFromConnectorConfig(connectorConfig, "snapshot.mode", "initial");
            config.put("snapshot.mode", snapshotMode);

            // Table filtering
            if (connectorConfig.has("table.include.list")) {
                config.put("table.include.list", connectorConfig.get("table.include.list").asText());
            }

            if (connectorConfig.has("table.exclude.list")) {
                config.put("table.exclude.list", connectorConfig.get("table.exclude.list").asText());
            }

            // Column filtering
            if (connectorConfig.has("column.include.list")) {
                config.put("column.include.list", connectorConfig.get("column.include.list").asText());
            }

            if (connectorConfig.has("column.exclude.list")) {
                config.put("column.exclude.list", connectorConfig.get("column.exclude.list").asText());
            }

            // Performance tuning
            config.put("snapshot.max.threads",
                    getFromConnectorConfig(connectorConfig, "snapshot.max.threads", "1"));
            config.put("max.batch.size",
                    getFromConnectorConfig(connectorConfig, "max.batch.size", "2048"));
            config.put("max.queue.size",
                    getFromConnectorConfig(connectorConfig, "max.queue.size", "8192"));

            // Offset and schema history storage
            config.put("offset.storage", "org.apache.kafka.connect.storage.KafkaOffsetBackingStore");
            config.put("offset.flush.interval.ms", "10000");

            // Schema history
            config.put("schema.history.internal.kafka.bootstrap.servers",
                    getFromConnectorConfig(connectorConfig, "kafka.bootstrap.servers", "localhost:9092"));
            config.put("schema.history.internal.kafka.topic",
                    task.getTaskCode() + "-schema-history");

            // Time zone
            if (sourceConfig.has("serverTimezone")) {
                config.put("database.serverTimezone", sourceConfig.get("serverTimezone").asText());
            }

            // SSL configuration
            if (sourceConfig.has("ssl") && sourceConfig.get("ssl").asBoolean()) {
                config.put("database.ssl.mode", "required");
            }

            // Include schema changes
            config.put("include.schema.changes", "true");

            // Decimal handling mode
            config.put("decimal.handling.mode", "precise");

            // Bigint unsigned handling
            config.put("bigint.unsigned.handling.mode", "long");

            // Time precision mode
            config.put("time.precision.mode", "adaptive");

            // Binary handling mode
            config.put("binary.handling.mode", "bytes");

            // Event processing
            config.put("tombstones.on.delete", "false");

            // Heartbeat
            config.put("heartbeat.interval.ms", "30000");
            config.put("heartbeat.topics.prefix", "__debezium-heartbeat");

            // Connector metadata
            config.put("key.converter", "org.apache.kafka.connect.json.JsonConverter");
            config.put("value.converter", "org.apache.kafka.connect.json.JsonConverter");
            config.put("key.converter.schemas.enable", "false");
            config.put("value.converter.schemas.enable", "false");

            log.debug("MySQL connector config built successfully: {}", config);
            return config;

        } catch (Exception e) {
            log.error("Failed to build MySQL connector config: {}", e.getMessage(), e);
            throw new BusinessException("Failed to build MySQL connector config: " + e.getMessage());
        }
    }

    @Override
    public boolean validateConnection(String connectionConfig) {
        log.debug("Validating MySQL connection");

        try {
            JsonNode config = JsonUtil.parseJson(connectionConfig);

            String host = config.get("host").asText();
            int port = config.has("port") ? config.get("port").asInt() : 3306;
            String database = config.get("database").asText();
            String username = config.get("username").asText();
            String password = config.get("password").asText();

            String jdbcUrl = String.format("jdbc:mysql://%s:%d/%s", host, port, database);

            // Try to establish connection
            try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password)) {
                log.info("MySQL connection validated successfully: {}:{}/{}", host, port, database);
                return true;
            }

        } catch (SQLException e) {
            log.error("MySQL connection validation failed: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Failed to parse connection config: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getConnectorClass() {
        return CONNECTOR_CLASS;
    }

    /**
     * Helper method to get value from connector config with default
     */
    private String getFromConnectorConfig(JsonNode config, String key, String defaultValue) {
        if (config.has(key)) {
            return config.get(key).asText();
        }
        return defaultValue;
    }
}
