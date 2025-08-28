package org.entcore.broker.api.dto.applications;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * DTO representing the status of an application.
 */
public class ApplicationStatusDTO {

    /**
     * Name of the application
     */
    private final String application;

    /**
     * Version of the application
     */
    private final String version;

    /**
     * Timestamp when the status was published
     */
    private final Instant timestamp;

    /**
     * Node identifier where the application is running
     */
    private final String nodeId;

    /**
     * Additional metadata about the application status
     */
    private final Map<String, Object> metadata;

    /**
     * Creates a new ApplicationStatusDTO with the specified parameters
     *
     * @param application Name of the application
     * @param version  Version of the application
     * @param nodeId   Node identifier where the application is running
     * @param metadata Additional metadata about the application status
     */
    @JsonCreator
    public ApplicationStatusDTO(
            @JsonProperty("application") String application,
            @JsonProperty("version") String version,
            @JsonProperty("nodeId") String nodeId,
            @JsonProperty("metadata") Map<String, Object> metadata) {
        this.application = application;
        this.version = version;
        this.timestamp = Instant.now();
        this.nodeId = nodeId;
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }

    /**
     * Creates a new ApplicationStatusDTO with minimal information
     *
     * @param application Name of the application
     * @param status Current status of the application
     * @param nodeId Node identifier where the application is running
     * @return A new ApplicationStatusDTO
     */
    public static ApplicationStatusDTO withBasicInfo(String application, String nodeId) {
        return new ApplicationStatusDTO(application, null, nodeId, null);
    }

    /**
     * Creates a new ApplicationStatusDTO with version information
     *
     * @param application Name of the application
     * @param status  Current status of the application
     * @param version Version of the application
     * @param nodeId  Node identifier where the application is running
     * @return A new ApplicationStatusDTO
     */
    public static ApplicationStatusDTO withVersion(String application, String version, String nodeId) {
        return new ApplicationStatusDTO(application, version, nodeId, null);
    }

    /**
     * @return Name of the application
     */
    public String getApplication() {
        return application;
    }

    /**
     * @return Version of the application
     */
    public String getVersion() {
        return version;
    }

    /**
     * @return Timestamp when the status was published
     */
    public Instant getTimestamp() {
        return timestamp;
    }

    /**
     * @return Node identifier where the application is running
     */
    public String getNodeId() {
        return nodeId;
    }

    /**
     * @return Additional metadata about the application status
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    @Override
    public String toString() {
        return "ApplicationStatusDTO{" +
                "application='" + application + '\'' +
                ", version='" + version + '\'' +
                ", timestamp=" + timestamp +
                ", nodeId='" + nodeId + '\'' +
                ", metadata=" + metadata +
                '}';
    }
}