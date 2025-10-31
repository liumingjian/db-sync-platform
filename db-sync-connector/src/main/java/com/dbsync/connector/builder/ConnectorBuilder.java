package com.dbsync.connector.builder;

import com.dbsync.core.domain.entity.SyncTask;

import java.util.Map;

/**
 * Connector configuration builder interface
 *
 * @author DB Sync Platform
 */
public interface ConnectorBuilder {

    /**
     * Build connector configuration from sync task
     *
     * @param task sync task entity
     * @return connector configuration map
     */
    Map<String, String> buildConfig(SyncTask task);

    /**
     * Validate connection configuration
     *
     * @param connectionConfig connection configuration JSON
     * @return true if valid, false otherwise
     */
    boolean validateConnection(String connectionConfig);

    /**
     * Get connector class name
     *
     * @return fully qualified connector class name
     */
    String getConnectorClass();
}
