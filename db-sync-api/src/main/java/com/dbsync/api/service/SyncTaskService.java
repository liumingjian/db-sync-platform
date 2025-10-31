package com.dbsync.api.service;

import com.dbsync.common.enums.HealthStatus;
import com.dbsync.common.enums.TaskStatus;
import com.dbsync.common.exceptions.BusinessException;
import com.dbsync.common.exceptions.ResourceNotFoundException;
import com.dbsync.connector.manager.ConnectorManager;
import com.dbsync.core.domain.entity.SyncTask;
import com.dbsync.core.repository.SyncTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Sync Task Service
 *
 * @author DB Sync Platform
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SyncTaskService {

    private final SyncTaskRepository syncTaskRepository;
    private final ConnectorManager connectorManager;

    /**
     * Create a new sync task
     */
    @Transactional
    public SyncTask createTask(SyncTask task) {
        log.info("Creating sync task: {}", task.getTaskCode());

        // Validate task code uniqueness within tenant
        if (syncTaskRepository.existsByTenantIdAndTaskCode(task.getTenantId(), task.getTaskCode())) {
            throw new BusinessException("Task code already exists: " + task.getTaskCode());
        }

        // Set initial status
        task.setStatus(TaskStatus.CREATED);
        task.setHealthStatus(HealthStatus.UNKNOWN);

        // Save task to database
        SyncTask savedTask = syncTaskRepository.save(task);

        log.info("Sync task created successfully: {} (ID: {})", savedTask.getTaskCode(), savedTask.getTaskId());
        return savedTask;
    }

    /**
     * Start a sync task
     */
    @Transactional
    public SyncTask startTask(UUID taskId) {
        log.info("Starting sync task: {}", taskId);

        SyncTask task = getTaskById(taskId);

        // Validate task can be started
        if (!task.canTransitionTo(TaskStatus.RUNNING)) {
            throw new BusinessException(
                    String.format("Cannot start task in %s status", task.getStatus()));
        }

        try {
            // Create connector if not exists
            if (task.getConnectorName() == null || !connectorManager.connectorExists(task.getConnectorName())) {
                String connectorName = connectorManager.createConnector(task);
                task.setConnectorName(connectorName);
            } else {
                // Start existing connector
                connectorManager.startConnector(task.getConnectorName());
            }

            // Update task status
            task.setStatus(TaskStatus.RUNNING);
            task.setHealthStatus(HealthStatus.HEALTHY);
            task.setLastError(null);

            SyncTask updatedTask = syncTaskRepository.save(task);

            log.info("Sync task started successfully: {}", task.getTaskCode());
            return updatedTask;

        } catch (Exception e) {
            log.error("Failed to start task {}: {}", task.getTaskCode(), e.getMessage(), e);

            // Update task with error
            task.setStatus(TaskStatus.FAILED);
            task.setHealthStatus(HealthStatus.UNHEALTHY);
            task.setLastError(e.getMessage());
            task.setErrorCount(task.getErrorCount() + 1);
            syncTaskRepository.save(task);

            throw new BusinessException("Failed to start task: " + e.getMessage());
        }
    }

    /**
     * Stop a sync task
     */
    @Transactional
    public SyncTask stopTask(UUID taskId) {
        log.info("Stopping sync task: {}", taskId);

        SyncTask task = getTaskById(taskId);

        // Validate task can be stopped
        if (!task.canTransitionTo(TaskStatus.STOPPED)) {
            throw new BusinessException(
                    String.format("Cannot stop task in %s status", task.getStatus()));
        }

        try {
            // Stop connector if exists
            if (task.getConnectorName() != null) {
                connectorManager.stopConnector(task.getConnectorName());
            }

            // Update task status
            task.setStatus(TaskStatus.STOPPED);
            task.setHealthStatus(HealthStatus.PAUSED);

            SyncTask updatedTask = syncTaskRepository.save(task);

            log.info("Sync task stopped successfully: {}", task.getTaskCode());
            return updatedTask;

        } catch (Exception e) {
            log.error("Failed to stop task {}: {}", task.getTaskCode(), e.getMessage(), e);
            throw new BusinessException("Failed to stop task: " + e.getMessage());
        }
    }

    /**
     * Pause a sync task
     */
    @Transactional
    public SyncTask pauseTask(UUID taskId) {
        log.info("Pausing sync task: {}", taskId);

        SyncTask task = getTaskById(taskId);

        // Validate task can be paused
        if (!task.canTransitionTo(TaskStatus.PAUSED)) {
            throw new BusinessException(
                    String.format("Cannot pause task in %s status", task.getStatus()));
        }

        try {
            // Pause connector if exists
            if (task.getConnectorName() != null) {
                connectorManager.stopConnector(task.getConnectorName());
            }

            // Update task status
            task.setStatus(TaskStatus.PAUSED);
            task.setHealthStatus(HealthStatus.PAUSED);

            SyncTask updatedTask = syncTaskRepository.save(task);

            log.info("Sync task paused successfully: {}", task.getTaskCode());
            return updatedTask;

        } catch (Exception e) {
            log.error("Failed to pause task {}: {}", task.getTaskCode(), e.getMessage(), e);
            throw new BusinessException("Failed to pause task: " + e.getMessage());
        }
    }

    /**
     * Resume a sync task
     */
    @Transactional
    public SyncTask resumeTask(UUID taskId) {
        log.info("Resuming sync task: {}", taskId);

        SyncTask task = getTaskById(taskId);

        // Validate task can be resumed
        if (task.getStatus() != TaskStatus.PAUSED) {
            throw new BusinessException("Only paused tasks can be resumed");
        }

        try {
            // Resume connector
            if (task.getConnectorName() != null) {
                connectorManager.startConnector(task.getConnectorName());
            }

            // Update task status
            task.setStatus(TaskStatus.RUNNING);
            task.setHealthStatus(HealthStatus.HEALTHY);

            SyncTask updatedTask = syncTaskRepository.save(task);

            log.info("Sync task resumed successfully: {}", task.getTaskCode());
            return updatedTask;

        } catch (Exception e) {
            log.error("Failed to resume task {}: {}", task.getTaskCode(), e.getMessage(), e);
            throw new BusinessException("Failed to resume task: " + e.getMessage());
        }
    }

    /**
     * Restart a sync task
     */
    @Transactional
    public SyncTask restartTask(UUID taskId) {
        log.info("Restarting sync task: {}", taskId);

        SyncTask task = getTaskById(taskId);

        try {
            // Restart connector if exists
            if (task.getConnectorName() != null) {
                connectorManager.restartConnector(task.getConnectorName());
            } else {
                // Create new connector if not exists
                String connectorName = connectorManager.createConnector(task);
                task.setConnectorName(connectorName);
            }

            // Update task status
            task.setStatus(TaskStatus.RUNNING);
            task.setHealthStatus(HealthStatus.HEALTHY);
            task.setLastError(null);

            SyncTask updatedTask = syncTaskRepository.save(task);

            log.info("Sync task restarted successfully: {}", task.getTaskCode());
            return updatedTask;

        } catch (Exception e) {
            log.error("Failed to restart task {}: {}", task.getTaskCode(), e.getMessage(), e);
            throw new BusinessException("Failed to restart task: " + e.getMessage());
        }
    }

    /**
     * Delete a sync task
     */
    @Transactional
    public void deleteTask(UUID taskId, boolean force) {
        log.info("Deleting sync task: {} (force: {})", taskId, force);

        SyncTask task = getTaskById(taskId);

        // Stop task if running and force delete
        if (force && task.getStatus() == TaskStatus.RUNNING) {
            try {
                stopTask(taskId);
            } catch (Exception e) {
                log.warn("Failed to stop task before deletion: {}", e.getMessage());
            }
        }

        // Cannot delete running task without force flag
        if (task.getStatus() == TaskStatus.RUNNING && !force) {
            throw new BusinessException("Cannot delete running task. Stop it first or use force delete.");
        }

        try {
            // Delete connector if exists
            if (task.getConnectorName() != null) {
                connectorManager.deleteConnector(task.getConnectorName());
            }

            // Soft delete task
            task.setDeletedAt(LocalDateTime.now());
            syncTaskRepository.save(task);

            log.info("Sync task deleted successfully: {}", task.getTaskCode());

        } catch (Exception e) {
            log.error("Failed to delete task {}: {}", task.getTaskCode(), e.getMessage(), e);
            throw new BusinessException("Failed to delete task: " + e.getMessage());
        }
    }

    /**
     * Update task configuration
     */
    @Transactional
    public SyncTask updateTask(UUID taskId, SyncTask updateRequest) {
        log.info("Updating sync task: {}", taskId);

        SyncTask task = getTaskById(taskId);

        // Cannot update running task's critical configurations
        if (task.getStatus() == TaskStatus.RUNNING) {
            throw new BusinessException("Cannot update running task. Stop it first.");
        }

        // Update allowed fields
        if (updateRequest.getTaskName() != null) {
            task.setTaskName(updateRequest.getTaskName());
        }

        if (updateRequest.getDescription() != null) {
            task.setDescription(updateRequest.getDescription());
        }

        if (updateRequest.getAlertConfig() != null) {
            task.setAlertConfig(updateRequest.getAlertConfig());
        }

        if (updateRequest.getScheduleConfig() != null) {
            task.setScheduleConfig(updateRequest.getScheduleConfig());
        }

        SyncTask updatedTask = syncTaskRepository.save(task);

        log.info("Sync task updated successfully: {}", task.getTaskCode());
        return updatedTask;
    }

    /**
     * Get task by ID
     */
    public SyncTask getTaskById(UUID taskId) {
        return syncTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));
    }

    /**
     * Get task by code
     */
    public SyncTask getTaskByCode(UUID tenantId, String taskCode) {
        return syncTaskRepository.findByTenantIdAndTaskCode(tenantId, taskCode)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskCode));
    }

    /**
     * Get all tasks for a tenant
     */
    public Page<SyncTask> getTasksByTenant(UUID tenantId, Pageable pageable) {
        return syncTaskRepository.findByTenantIdAndDeletedAtIsNull(tenantId, pageable);
    }

    /**
     * Get tasks by status
     */
    public Page<SyncTask> getTasksByStatus(TaskStatus status, Pageable pageable) {
        return syncTaskRepository.findByStatusAndDeletedAtIsNull(status, pageable);
    }

    /**
     * Get tasks by health status
     */
    public List<SyncTask> getTasksByHealthStatus(HealthStatus healthStatus) {
        return syncTaskRepository.findByHealthStatusAndDeletedAtIsNull(healthStatus);
    }

    /**
     * Update task health status
     */
    @Transactional
    public void updateTaskHealth(UUID taskId) {
        log.debug("Updating health status for task: {}", taskId);

        SyncTask task = getTaskById(taskId);

        if (task.getConnectorName() == null) {
            return;
        }

        try {
            ConnectorManager.ConnectorHealthInfo healthInfo =
                    connectorManager.getConnectorHealth(task.getConnectorName());

            task.setHealthStatus(healthInfo.getHealthStatus());

            if (healthInfo.getHealthStatus() == HealthStatus.UNHEALTHY) {
                task.setLastError(healthInfo.getMessage());
                task.setErrorCount(task.getErrorCount() + 1);
            }

            syncTaskRepository.save(task);

        } catch (Exception e) {
            log.error("Failed to update task health: {}", e.getMessage());
        }
    }

    /**
     * Update task sync statistics
     */
    @Transactional
    public void updateTaskStats(UUID taskId, long recordsSynced) {
        log.debug("Updating sync statistics for task: {}", taskId);

        SyncTask task = getTaskById(taskId);

        task.setTotalRecordsSynced(task.getTotalRecordsSynced() + recordsSynced);
        task.setLastSyncTime(LocalDateTime.now());

        syncTaskRepository.save(task);
    }
}
