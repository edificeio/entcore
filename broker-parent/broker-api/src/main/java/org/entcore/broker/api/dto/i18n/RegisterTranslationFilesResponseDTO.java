package org.entcore.broker.api.dto.i18n;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This class represents a response to a request to register I18n files.
 */
public class RegisterTranslationFilesResponseDTO {
    
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
    private final int translationsCount;
    
    /**
     * Creates a new instance of RegisterI18nFilesResponseDTO.
     *
     * @param application The application name
     * @param languagesCount The number of languages registered
     * @param translationsCount The number of translation keys registered
     */
    @JsonCreator
    public RegisterTranslationFilesResponseDTO(
            @JsonProperty("application") String application,
            @JsonProperty("languagesCount") int languagesCount,
            @JsonProperty("translationsCount") int translationsCount) {
        this.application = application;
        this.languagesCount = languagesCount;
        this.translationsCount = translationsCount;
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
    public int getTranslationsCount() {
        return translationsCount;
    }
}