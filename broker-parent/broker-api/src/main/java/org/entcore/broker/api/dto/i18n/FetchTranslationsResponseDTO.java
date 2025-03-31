package org.entcore.broker.api.dto.i18n;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * This class represents a response containing translations for a specific language and theme.
 * It contains a map of translation keys to their corresponding values in the requested language.
 */
public class FetchTranslationsResponseDTO {

    /**
     * A map of translation keys to their corresponding values in the requested language.
     * This will be null if the fetch operation fails.
     */
    private final Map<String, String> translations;

    /**
     * Creates a new instance of FetchTranslationsResponseDTO.
     *
     * @param translations A map of translation keys to their corresponding values.
     */
    @JsonCreator
    public FetchTranslationsResponseDTO(@JsonProperty("translations") Map<String, String> translations) {
        this.translations = translations;
    }

    /**
     * Gets a map of translation keys to their corresponding values in the requested language.
     * @return A map of translations, or null if the fetch operation failed.
     */
    public Map<String, String> getTranslations() {
        return translations;
    }

    @Override
    public String toString() {
        return "FetchTranslationsResponseDTO{" +
                " translationsCount=" + (translations != null ? translations.size() : 0) +
                '}';
    }
}