package org.entcore.broker.api.dto.timeline;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data Transfer Object for the response to a timeline i18n registration request.
 */
public class RegisterTimelineI18nResponseDTO {
    
    /**
     * The application name for which translations were registered.
     */
    private final String application;
    
    /**
     * The number of languages registered.
     */
    private final int languagesCount;
    
    /**
     * The number of translation keys registered.
     */
    private final int keysCount;
    
    /**
     * Creates a new instance of RegisterTimelineI18nResponseDTO.
     *
     * @param application The application name
     * @param languagesCount The number of languages registered
     * @param keysCount The number of translation keys registered
     */
    @JsonCreator
    public RegisterTimelineI18nResponseDTO(
            @JsonProperty("application") String application,
            @JsonProperty("languagesCount") int languagesCount,
            @JsonProperty("keysCount") int keysCount) {
        this.application = application;
        this.languagesCount = languagesCount;
        this.keysCount = keysCount;
    }
    
    /**
     * Gets the application name.
     * @return The application name
     */
    public String getApplication() {
        return application;
    }
    
    /**
     * Gets the number of languages registered.
     * @return The number of languages registered
     */
    public int getLanguagesCount() {
        return languagesCount;
    }
    
    /**
     * Gets the number of translation keys registered.
     * @return The number of translation keys registered
     */
    public int getKeysCount() {
        return keysCount;
    }
    
    @Override
    public String toString() {
        return "RegisterTimelineI18nResponseDTO{" +
                "application='" + application + '\'' +
                ", languagesCount=" + languagesCount +
                ", keysCount=" + keysCount +
                '}';
    }
}