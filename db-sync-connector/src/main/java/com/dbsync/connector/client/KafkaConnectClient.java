package com.dbsync.connector.client;

import com.dbsync.common.exceptions.BusinessException;
import com.dbsync.common.utils.JsonUtil;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Kafka Connect REST API Client
 *
 * @author DB Sync Platform
 */
@Slf4j
@Component
public class KafkaConnectClient {

    private final RestTemplate restTemplate;
    private final String kafkaConnectUrl;

    public KafkaConnectClient(
            RestTemplate restTemplate,
            @Value("${kafka.connect.url:http://localhost:8083}") String kafkaConnectUrl) {
        this.restTemplate = restTemplate;
        this.kafkaConnectUrl = kafkaConnectUrl;
    }

    /**
     * Create a new connector
     */
    public ConnectorInfo createConnector(String connectorName, Map<String, String> config) {
        log.info("Creating connector: {}", connectorName);

        String url = kafkaConnectUrl + "/connectors";

        Map<String, Object> requestBody = Map.of(
                "name", connectorName,
                "config", config
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseConnectorInfo(response.getBody());
            }

            throw new BusinessException("Failed to create connector: " + connectorName);

        } catch (HttpClientErrorException e) {
            log.error("Failed to create connector {}: {} - {}",
                    connectorName, e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException("Failed to create connector: " + e.getMessage());
        } catch (RestClientException e) {
            log.error("Failed to connect to Kafka Connect: {}", e.getMessage());
            throw new BusinessException("Failed to connect to Kafka Connect: " + e.getMessage());
        }
    }

    /**
     * Get connector information
     */
    public ConnectorInfo getConnectorInfo(String connectorName) {
        log.debug("Getting connector info: {}", connectorName);

        String url = kafkaConnectUrl + "/connectors/" + connectorName;

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseConnectorInfo(response.getBody());
            }

            throw new BusinessException("Connector not found: " + connectorName);

        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Connector not found: {}", connectorName);
            return null;
        } catch (RestClientException e) {
            log.error("Failed to get connector info: {}", e.getMessage());
            throw new BusinessException("Failed to get connector info: " + e.getMessage());
        }
    }

    /**
     * Get connector status
     */
    public ConnectorStatus getConnectorStatus(String connectorName) {
        log.debug("Getting connector status: {}", connectorName);

        String url = kafkaConnectUrl + "/connectors/" + connectorName + "/status";

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseConnectorStatus(response.getBody());
            }

            throw new BusinessException("Failed to get connector status: " + connectorName);

        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Connector not found: {}", connectorName);
            return null;
        } catch (RestClientException e) {
            log.error("Failed to get connector status: {}", e.getMessage());
            throw new BusinessException("Failed to get connector status: " + e.getMessage());
        }
    }

    /**
     * Update connector configuration
     */
    public ConnectorInfo updateConnectorConfig(String connectorName, Map<String, String> config) {
        log.info("Updating connector config: {}", connectorName);

        String url = kafkaConnectUrl + "/connectors/" + connectorName + "/config";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(config, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.PUT, request, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseConnectorInfo(response.getBody());
            }

            throw new BusinessException("Failed to update connector config: " + connectorName);

        } catch (RestClientException e) {
            log.error("Failed to update connector config: {}", e.getMessage());
            throw new BusinessException("Failed to update connector config: " + e.getMessage());
        }
    }

    /**
     * Delete connector
     */
    public void deleteConnector(String connectorName) {
        log.info("Deleting connector: {}", connectorName);

        String url = kafkaConnectUrl + "/connectors/" + connectorName;

        try {
            restTemplate.delete(url);
            log.info("Connector deleted successfully: {}", connectorName);

        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Connector not found for deletion: {}", connectorName);
        } catch (RestClientException e) {
            log.error("Failed to delete connector: {}", e.getMessage());
            throw new BusinessException("Failed to delete connector: " + e.getMessage());
        }
    }

    /**
     * Pause connector
     */
    public void pauseConnector(String connectorName) {
        log.info("Pausing connector: {}", connectorName);

        String url = kafkaConnectUrl + "/connectors/" + connectorName + "/pause";

        try {
            restTemplate.put(url, null);
            log.info("Connector paused successfully: {}", connectorName);

        } catch (RestClientException e) {
            log.error("Failed to pause connector: {}", e.getMessage());
            throw new BusinessException("Failed to pause connector: " + e.getMessage());
        }
    }

    /**
     * Resume connector
     */
    public void resumeConnector(String connectorName) {
        log.info("Resuming connector: {}", connectorName);

        String url = kafkaConnectUrl + "/connectors/" + connectorName + "/resume";

        try {
            restTemplate.put(url, null);
            log.info("Connector resumed successfully: {}", connectorName);

        } catch (RestClientException e) {
            log.error("Failed to resume connector: {}", e.getMessage());
            throw new BusinessException("Failed to resume connector: " + e.getMessage());
        }
    }

    /**
     * Restart connector
     */
    public void restartConnector(String connectorName) {
        log.info("Restarting connector: {}", connectorName);

        String url = kafkaConnectUrl + "/connectors/" + connectorName + "/restart";

        try {
            restTemplate.postForEntity(url, null, String.class);
            log.info("Connector restarted successfully: {}", connectorName);

        } catch (RestClientException e) {
            log.error("Failed to restart connector: {}", e.getMessage());
            throw new BusinessException("Failed to restart connector: " + e.getMessage());
        }
    }

    /**
     * List all connectors
     */
    public List<String> listConnectors() {
        log.debug("Listing all connectors");

        String url = kafkaConnectUrl + "/connectors";

        try {
            ResponseEntity<String[]> response = restTemplate.getForEntity(url, String[].class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return List.of(response.getBody());
            }

            return new ArrayList<>();

        } catch (RestClientException e) {
            log.error("Failed to list connectors: {}", e.getMessage());
            throw new BusinessException("Failed to list connectors: " + e.getMessage());
        }
    }

    /**
     * Validate connector configuration
     */
    public ValidationResult validateConnectorConfig(String connectorClass, Map<String, String> config) {
        log.debug("Validating connector config for class: {}", connectorClass);

        String url = kafkaConnectUrl + "/connector-plugins/" + connectorClass + "/config/validate";

        Map<String, Object> requestBody = Map.of("connector.class", connectorClass);
        requestBody.putAll(config);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseValidationResult(response.getBody());
            }

            throw new BusinessException("Failed to validate connector config");

        } catch (RestClientException e) {
            log.error("Failed to validate connector config: {}", e.getMessage());
            throw new BusinessException("Failed to validate connector config: " + e.getMessage());
        }
    }

    /**
     * Parse connector info from JSON response
     */
    private ConnectorInfo parseConnectorInfo(String json) {
        try {
            JsonNode root = JsonUtil.parseJson(json);

            ConnectorInfo info = new ConnectorInfo();
            info.setName(root.get("name").asText());

            if (root.has("config")) {
                JsonNode configNode = root.get("config");
                Map<String, String> config = JsonUtil.fromJson(configNode.toString(), Map.class);
                info.setConfig(config);
            }

            if (root.has("tasks")) {
                JsonNode tasksNode = root.get("tasks");
                List<TaskInfo> tasks = new ArrayList<>();
                for (JsonNode taskNode : tasksNode) {
                    TaskInfo task = new TaskInfo();
                    task.setConnector(taskNode.get("connector").asText());
                    task.setTask(taskNode.get("task").asInt());
                    tasks.add(task);
                }
                info.setTasks(tasks);
            }

            if (root.has("type")) {
                info.setType(root.get("type").asText());
            }

            return info;

        } catch (Exception e) {
            log.error("Failed to parse connector info: {}", e.getMessage());
            throw new BusinessException("Failed to parse connector info: " + e.getMessage());
        }
    }

    /**
     * Parse connector status from JSON response
     */
    private ConnectorStatus parseConnectorStatus(String json) {
        try {
            JsonNode root = JsonUtil.parseJson(json);

            ConnectorStatus status = new ConnectorStatus();
            status.setName(root.get("name").asText());

            if (root.has("connector")) {
                JsonNode connectorNode = root.get("connector");
                status.setState(connectorNode.get("state").asText());
                if (connectorNode.has("worker_id")) {
                    status.setWorkerId(connectorNode.get("worker_id").asText());
                }
            }

            if (root.has("tasks")) {
                JsonNode tasksNode = root.get("tasks");
                List<TaskStatus> tasks = new ArrayList<>();
                for (JsonNode taskNode : tasksNode) {
                    TaskStatus task = new TaskStatus();
                    task.setId(taskNode.get("id").asInt());
                    task.setState(taskNode.get("state").asText());
                    if (taskNode.has("worker_id")) {
                        task.setWorkerId(taskNode.get("worker_id").asText());
                    }
                    if (taskNode.has("trace")) {
                        task.setTrace(taskNode.get("trace").asText());
                    }
                    tasks.add(task);
                }
                status.setTasks(tasks);
            }

            return status;

        } catch (Exception e) {
            log.error("Failed to parse connector status: {}", e.getMessage());
            throw new BusinessException("Failed to parse connector status: " + e.getMessage());
        }
    }

    /**
     * Parse validation result from JSON response
     */
    private ValidationResult parseValidationResult(String json) {
        try {
            JsonNode root = JsonUtil.parseJson(json);

            ValidationResult result = new ValidationResult();
            result.setName(root.get("name").asText());

            int errorCount = root.get("error_count").asInt();
            result.setErrorCount(errorCount);
            result.setValid(errorCount == 0);

            if (root.has("configs")) {
                // Parse validation details if needed
                result.setDetails(root.get("configs").toString());
            }

            return result;

        } catch (Exception e) {
            log.error("Failed to parse validation result: {}", e.getMessage());
            throw new BusinessException("Failed to parse validation result: " + e.getMessage());
        }
    }

    // Inner classes for response objects

    public static class ConnectorInfo {
        private String name;
        private String type;
        private Map<String, String> config;
        private List<TaskInfo> tasks;

        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public Map<String, String> getConfig() { return config; }
        public void setConfig(Map<String, String> config) { this.config = config; }
        public List<TaskInfo> getTasks() { return tasks; }
        public void setTasks(List<TaskInfo> tasks) { this.tasks = tasks; }
    }

    public static class TaskInfo {
        private String connector;
        private int task;

        public String getConnector() { return connector; }
        public void setConnector(String connector) { this.connector = connector; }
        public int getTask() { return task; }
        public void setTask(int task) { this.task = task; }
    }

    public static class ConnectorStatus {
        private String name;
        private String state;
        private String workerId;
        private List<TaskStatus> tasks;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getState() { return state; }
        public void setState(String state) { this.state = state; }
        public String getWorkerId() { return workerId; }
        public void setWorkerId(String workerId) { this.workerId = workerId; }
        public List<TaskStatus> getTasks() { return tasks; }
        public void setTasks(List<TaskStatus> tasks) { this.tasks = tasks; }
    }

    public static class TaskStatus {
        private int id;
        private String state;
        private String workerId;
        private String trace;

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public String getState() { return state; }
        public void setState(String state) { this.state = state; }
        public String getWorkerId() { return workerId; }
        public void setWorkerId(String workerId) { this.workerId = workerId; }
        public String getTrace() { return trace; }
        public void setTrace(String trace) { this.trace = trace; }
    }

    public static class ValidationResult {
        private String name;
        private int errorCount;
        private boolean valid;
        private String details;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getErrorCount() { return errorCount; }
        public void setErrorCount(int errorCount) { this.errorCount = errorCount; }
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        public String getDetails() { return details; }
        public void setDetails(String details) { this.details = details; }
    }
}
