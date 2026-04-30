package org.entcore.broker.api.dto.timeline;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.beans.Transient;

import java.util.Map;
import java.io.IOException;

/**
 * Data Transfer Object for registering timeline i18n files.
 */
public class RegisterTimelineI18nRequestDTO {
    
    /**
     * The application name for which to register translations.
     */
    private final String application;
    
    /**
     * A map of language files to their key-value translation pairs.
     * Format: {"fr.json": {"hello": "bonjour"}, "en.json": {"hello": "hello"}}
     */
    private final Map<String, Map<String, String>> translationsByLanguage;
    
    /**
     * Creates a new instance of RegisterTimelineI18nRequestDTO.
     *
     * @param application The application name
     * @param translationsByLanguage A map of language files to their key-value translation pairs
     */
    @JsonCreator
    public RegisterTimelineI18nRequestDTO(
            @JsonProperty("application") String application,
            @JsonProperty("translationsByLanguage") Map<String, Map<String, String>> translationsByLanguage) {
        this.application = application;
        this.translationsByLanguage = translationsByLanguage;
    }
    
    /**
     * Gets the application name.
     * @return The application name
     */
    public String getApplication() {
        return application;
    }
    
    /**
     * Gets the translations by language map.
     * @return A map of language files to their key-value translation pairs
     */
    public Map<String, Map<String, String>> getTranslationsByLanguage() {
        return translationsByLanguage;
    }
    
    /**
     * Validates the request.
     * @return true if the request is valid, false otherwise
     */
    @Transient()
    public boolean isValid() {
        return application != null && !application.isEmpty() && 
               translationsByLanguage != null && !translationsByLanguage.isEmpty();
    }
    
    @Override
    public String toString() {
        return "RegisterTimelineI18nRequestDTO{" +
                "application='" + application + '\'' +
                ", translationsByLanguage=" + translationsByLanguage +
                '}';
    }
}