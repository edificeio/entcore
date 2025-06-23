package org.entcore.broker.api.dto.i18n;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.Map;
import java.io.IOException;

/**
 * This class represents a request to register I18n files for an application.
 */
public class RegisterTranslationFilesRequestDTO {
    
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
     * Creates a new instance of RegisterI18nFilesRequestDTO.
     *
     * @param application The application name
     * @param translationsByLanguage A map of language files to their key-value translation pairs
     */
    @JsonCreator
    public RegisterTranslationFilesRequestDTO(
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
    public boolean isValid() {
        return application != null && !application.isEmpty() && 
               translationsByLanguage != null && !translationsByLanguage.isEmpty();
    }
    
    /**
     * Deserializes a JSON string into a RegisterTranslationFilesRequestDTO object
     *
     * @param json The JSON string to deserialize
     * @return The deserialized RegisterTranslationFilesRequestDTO object
     * @throws IOException If there is an error during deserialization
     */
    public static RegisterTranslationFilesRequestDTO fromJSON(String json) throws IOException {
        if (json == null || json.isEmpty()) {
            throw new IllegalArgumentException("JSON string cannot be null or empty");
        }
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper.readValue(json, RegisterTranslationFilesRequestDTO.class);
    }
    
    /**
     * Serializes this object into a JSON string
     *
     * @return The JSON string representation of this object
     * @throws JsonProcessingException If there is an error during serialization
     */
    public String toJSON() throws JsonProcessingException {
        final ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(this);
    }
}