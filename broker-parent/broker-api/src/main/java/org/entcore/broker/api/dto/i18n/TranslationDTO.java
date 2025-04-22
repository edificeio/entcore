package org.entcore.broker.api.dto.i18n;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This class represents a single translation entry.
 * It contains a key and its corresponding translated value.
 */
public class TranslationDTO {
    /**
     * The key for this translation.
     * This is typically a dot-separated identifier for the translated text.
     */
    private final String key;

    /**
     * The translated value for the key in the requested language.
     */
    private final String value;

    /**
     * Creates a new instance of TranslationDTO.
     *
     * @param key The key for this translation.
     * @param value The translated value for the key.
     */
    @JsonCreator
    public TranslationDTO(
            @JsonProperty("key") String key,
            @JsonProperty("value") String value) {
        this.key = key;
        this.value = value;
    }

    /**
     * Gets the key for this translation.
     * @return The translation key.
     */
    public String getKey() {
        return key;
    }

    /**
     * Gets the translated value for the key in the requested language.
     * @return The translated value.
     */
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "TranslationDTO{" +
                "key='" + key + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}