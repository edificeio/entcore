package org.entcore.broker.api.dto.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.beans.Transient;
import io.vertx.core.json.JsonObject;

import java.util.Map;

/**
 * Data Transfer Object for event creation requests.
 * Contains all necessary information to create and store an event.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateEventRequestDTO {
    private final String eventType;
    private final String userId;
    private final String login;
    private final String clientId;
    private final JsonObject customAttributes;
    private final Map<String, String> headers;
    private final String ip;
    private final String userAgent;
    private final String path;
    private final String module;
    private final String resourceType;
    
    /**
     * Constructor with all fields
     */
    @JsonCreator
    public CreateEventRequestDTO(
            @JsonProperty("eventType") String eventType,
            @JsonProperty("userId") String userId,
            @JsonProperty("login") String login,
            @JsonProperty("clientId") String clientId,
            @JsonProperty("customAttributes") JsonObject customAttributes,
            @JsonProperty("headers") Map<String, String> headers,
            @JsonProperty("ip") String ip,
            @JsonProperty("userAgent") String userAgent,
            @JsonProperty("path") String path,
            @JsonProperty("module") String module,
            @JsonProperty("resourceType") String resourceType) {
        this.eventType = eventType;
        this.userId = userId;
        this.login = login;
        this.clientId = clientId;
        this.customAttributes = customAttributes;
        this.headers = headers;
        this.ip = ip;
        this.userAgent = userAgent;
        this.path = path;
        this.module = module;
        this.resourceType = resourceType;
    }
    
    /**
     * Validates that the request contains the minimum required fields
     * @return true if the request is valid, false otherwise
     */
    @Transient
    public boolean isValid() {
        // At minimum, an event type and module are required
        return eventType != null && !eventType.isEmpty() && 
               module != null && !module.isEmpty();
    }

    // Getters only, with JsonProperty annotations
    @JsonProperty("eventType")
    public String getEventType() {
        return eventType;
    }

    @JsonProperty("userId")
    public String getUserId() {
        return userId;
    }

    @JsonProperty("login")
    public String getLogin() {
        return login;
    }

    @JsonProperty("clientId")
    public String getClientId() {
        return clientId;
    }

    @JsonProperty("customAttributes")
    public JsonObject getCustomAttributes() {
        return customAttributes;
    }

    @JsonProperty("headers")
    public Map<String, String> getHeaders() {
        return headers;
    }

    @JsonProperty("ip")
    public String getIp() {
        return ip;
    }

    @JsonProperty("userAgent")
    public String getUserAgent() {
        return userAgent;
    }

    @JsonProperty("path")
    public String getPath() {
        return path;
    }
    
    @JsonProperty("module")
    public String getModule() {
        return module;
    }

    @JsonProperty("resourceType")
    public String getResourceType() {
        return resourceType;
    }
    
    @Override
    public String toString() {
        return "CreateEventRequestDTO{" +
                "eventType='" + eventType + '\'' +
                ", userId='" + (userId != null ? userId : "null") + '\'' +
                ", login='" + (login != null ? login : "null") + '\'' +
                ", clientId='" + (clientId != null ? clientId : "null") + '\'' +
                ", customAttributes=" + (customAttributes != null ? "present" : "null") +
                ", headers=" + (headers != null ? "present" : "null") +
                ", ip='" + (ip != null ? ip : "null") + '\'' +
                ", path='" + (path != null ? path : "null") + '\'' +
                ", module='" + module + '\'' +
                ", resourceType='" + (resourceType != null ? resourceType : "null") + '\'' +
                '}';
    }
}