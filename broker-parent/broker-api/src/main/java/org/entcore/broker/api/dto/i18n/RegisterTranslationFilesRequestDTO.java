package org.entcore.broker.api.dto.i18n;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

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
}