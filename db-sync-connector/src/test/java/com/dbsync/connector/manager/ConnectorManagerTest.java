package com.dbsync.connector.manager;

import com.dbsync.common.enums.DatabaseType;
import com.dbsync.common.enums.HealthStatus;
import com.dbsync.common.enums.SyncMode;
import com.dbsync.common.enums.TaskStatus;
import com.dbsync.common.exceptions.BusinessException;
import com.dbsync.connector.builder.MySQLConnectorBuilder;
import com.dbsync.connector.client.KafkaConnectClient;
import com.dbsync.core.domain.entity.SyncTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ConnectorManager
 *
 * @author DB Sync Platform
 */
@ExtendWith(MockitoExtension.class)
class ConnectorManagerTest {

    @Mock
    private KafkaConnectClient connectClient;

    @Mock
    private MySQLConnectorBuilder mysqlConnectorBuilder;

    private ConnectorManager connectorManager;

    private SyncTask testTask;
    private UUID testTaskId;
    private UUID testTenantId;

    @BeforeEach
    void setUp() {
        testTaskId = UUID.randomUUID();
        testTenantId = UUID.randomUUID();

        testTask = new SyncTask();
        testTask.setTaskId(testTaskId);
        testTask.setTenantId(testTenantId);
        testTask.setTaskName("Test Sync Task");
        testTask.setTaskCode("test-sync-001");
        testTask.setSourceDbType(DatabaseType.MYSQL);
        testTask.setTargetDbType(DatabaseType.POSTGRESQL);
        testTask.setSourceConnectionConfig("{\"host\":\"localhost\",\"port\":3306,\"database\":\"testdb\",\"username\":\"user\",\"password\":\"pass\"}");
        testTask.setTargetConnectionConfig("{\"host\":\"localhost\",\"port\":5432}");
        testTask.setConnectorConfig("{}");
        testTask.setSyncMode(SyncMode.FULL_INCREMENTAL);
        testTask.setStatus(TaskStatus.CREATED);

        connectorManager = new ConnectorManager(connectClient, mysqlConnectorBuilder);
    }

    @Test
    void testCreateConnector_Success() {
        // Given
        String connectorName = "test-sync-001-connector";
        Map<String, String> config = new HashMap<>();
        config.put("connector.class", "io.debezium.connector.mysql.MySqlConnector");

        KafkaConnectClient.ConnectorInfo connectorInfo = new KafkaConnectClient.ConnectorInfo();
        connectorInfo.setName(connectorName);

        when(mysqlConnectorBuilder.validateConnection(testTask.getSourceConnectionConfig()))
                .thenReturn(true);
        when(mysqlConnectorBuilder.buildConfig(testTask)).thenReturn(config);
        when(connectClient.createConnector(eq(connectorName), any())).thenReturn(connectorInfo);

        // When
        String result = connectorManager.createConnector(testTask);

        // Then
        assertThat(result).isEqualTo(connectorName);
        verify(mysqlConnectorBuilder).validateConnection(testTask.getSourceConnectionConfig());
        verify(mysqlConnectorBuilder).buildConfig(testTask);
        verify(connectClient).createConnector(eq(connectorName), any());
    }

    @Test
    void testCreateConnector_ConnectionValidationFailed() {
        // Given
        when(mysqlConnectorBuilder.validateConnection(testTask.getSourceConnectionConfig()))
                .thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> connectorManager.createConnector(testTask))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Failed to validate source database connection");

        verify(mysqlConnectorBuilder).validateConnection(testTask.getSourceConnectionConfig());
        verify(mysqlConnectorBuilder, never()).buildConfig(any());
        verify(connectClient, never()).createConnector(any(), any());
    }

    @Test
    void testCreateConnector_UnsupportedDatabaseType() {
        // Given
        testTask.setSourceDbType(DatabaseType.ORACLE);

        // When & Then
        assertThatThrownBy(() -> connectorManager.createConnector(testTask))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Unsupported database type");
    }

    @Test
    void testGetConnectorHealth_Healthy() {
        // Given
        String connectorName = "test-connector";
        KafkaConnectClient.ConnectorStatus status = new KafkaConnectClient.ConnectorStatus();
        status.setName(connectorName);
        status.setState("RUNNING");

        List<KafkaConnectClient.TaskStatus> tasks = new ArrayList<>();
        KafkaConnectClient.TaskStatus taskStatus = new KafkaConnectClient.TaskStatus();
        taskStatus.setId(0);
        taskStatus.setState("RUNNING");
        tasks.add(taskStatus);
        status.setTasks(tasks);

        when(connectClient.getConnectorStatus(connectorName)).thenReturn(status);

        // When
        ConnectorManager.ConnectorHealthInfo healthInfo = connectorManager.getConnectorHealth(connectorName);

        // Then
        assertThat(healthInfo).isNotNull();
        assertThat(healthInfo.getHealthStatus()).isEqualTo(HealthStatus.HEALTHY);
        verify(connectClient).getConnectorStatus(connectorName);
    }

    @Test
    void testGetConnectorHealth_Degraded() {
        // Given
        String connectorName = "test-connector";
        KafkaConnectClient.ConnectorStatus status = new KafkaConnectClient.ConnectorStatus();
        status.setName(connectorName);
        status.setState("RUNNING");

        List<KafkaConnectClient.TaskStatus> tasks = new ArrayList<>();
        KafkaConnectClient.TaskStatus taskStatus1 = new KafkaConnectClient.TaskStatus();
        taskStatus1.setId(0);
        taskStatus1.setState("RUNNING");
        tasks.add(taskStatus1);

        KafkaConnectClient.TaskStatus taskStatus2 = new KafkaConnectClient.TaskStatus();
        taskStatus2.setId(1);
        taskStatus2.setState("FAILED");
        tasks.add(taskStatus2);

        status.setTasks(tasks);

        when(connectClient.getConnectorStatus(connectorName)).thenReturn(status);

        // When
        ConnectorManager.ConnectorHealthInfo healthInfo = connectorManager.getConnectorHealth(connectorName);

        // Then
        assertThat(healthInfo).isNotNull();
        assertThat(healthInfo.getHealthStatus()).isEqualTo(HealthStatus.DEGRADED);
        verify(connectClient).getConnectorStatus(connectorName);
    }

    @Test
    void testGetConnectorHealth_Unhealthy() {
        // Given
        String connectorName = "test-connector";
        KafkaConnectClient.ConnectorStatus status = new KafkaConnectClient.ConnectorStatus();
        status.setName(connectorName);
        status.setState("FAILED");
        status.setTasks(new ArrayList<>());

        when(connectClient.getConnectorStatus(connectorName)).thenReturn(status);

        // When
        ConnectorManager.ConnectorHealthInfo healthInfo = connectorManager.getConnectorHealth(connectorName);

        // Then
        assertThat(healthInfo).isNotNull();
        assertThat(healthInfo.getHealthStatus()).isEqualTo(HealthStatus.UNHEALTHY);
        verify(connectClient).getConnectorStatus(connectorName);
    }

    @Test
    void testGetConnectorHealth_NotFound() {
        // Given
        String connectorName = "non-existent-connector";
        when(connectClient.getConnectorStatus(connectorName)).thenReturn(null);

        // When
        ConnectorManager.ConnectorHealthInfo healthInfo = connectorManager.getConnectorHealth(connectorName);

        // Then
        assertThat(healthInfo).isNotNull();
        assertThat(healthInfo.getHealthStatus()).isEqualTo(HealthStatus.UNKNOWN);
        assertThat(healthInfo.getMessage()).contains("Connector not found");
        verify(connectClient).getConnectorStatus(connectorName);
    }

    @Test
    void testStopConnector_Success() {
        // Given
        String connectorName = "test-connector";
        doNothing().when(connectClient).pauseConnector(connectorName);

        // When
        connectorManager.stopConnector(connectorName);

        // Then
        verify(connectClient).pauseConnector(connectorName);
    }

    @Test
    void testStartConnector_Success() {
        // Given
        String connectorName = "test-connector";
        KafkaConnectClient.ConnectorInfo connectorInfo = new KafkaConnectClient.ConnectorInfo();
        connectorInfo.setName(connectorName);

        when(connectClient.getConnectorInfo(connectorName)).thenReturn(connectorInfo);
        doNothing().when(connectClient).resumeConnector(connectorName);

        // When
        connectorManager.startConnector(connectorName);

        // Then
        verify(connectClient).getConnectorInfo(connectorName);
        verify(connectClient).resumeConnector(connectorName);
    }

    @Test
    void testStartConnector_NotFound() {
        // Given
        String connectorName = "non-existent-connector";
        when(connectClient.getConnectorInfo(connectorName)).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> connectorManager.startConnector(connectorName))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Connector not found");

        verify(connectClient).getConnectorInfo(connectorName);
        verify(connectClient, never()).resumeConnector(any());
    }

    @Test
    void testRestartConnector_Success() {
        // Given
        String connectorName = "test-connector";
        doNothing().when(connectClient).restartConnector(connectorName);

        // When
        connectorManager.restartConnector(connectorName);

        // Then
        verify(connectClient).restartConnector(connectorName);
    }

    @Test
    void testDeleteConnector_Success() {
        // Given
        String connectorName = "test-connector";
        doNothing().when(connectClient).deleteConnector(connectorName);

        // When
        connectorManager.deleteConnector(connectorName);

        // Then
        verify(connectClient).deleteConnector(connectorName);
    }

    @Test
    void testConnectorExists_True() {
        // Given
        String connectorName = "test-connector";
        KafkaConnectClient.ConnectorInfo connectorInfo = new KafkaConnectClient.ConnectorInfo();
        connectorInfo.setName(connectorName);

        when(connectClient.getConnectorInfo(connectorName)).thenReturn(connectorInfo);

        // When
        boolean exists = connectorManager.connectorExists(connectorName);

        // Then
        assertThat(exists).isTrue();
        verify(connectClient).getConnectorInfo(connectorName);
    }

    @Test
    void testConnectorExists_False() {
        // Given
        String connectorName = "non-existent-connector";
        when(connectClient.getConnectorInfo(connectorName)).thenReturn(null);

        // When
        boolean exists = connectorManager.connectorExists(connectorName);

        // Then
        assertThat(exists).isFalse();
        verify(connectClient).getConnectorInfo(connectorName);
    }
}
