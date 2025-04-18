package org.entcore.broker.api.dto.i18n;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * This class represents a request to fetch translations.
 * It can either contain HTTP headers or explicit language and domain information.
 */
public class FetchTranslationsRequestDTO {
    /**
     * The headers of the HTTP request.
     * These will be used to extract language and theme information.
     */
    private final Map<String, String> headers;

    /**
     * The explicit language and domain information.
     * This is used when headers are not provided.
     */
    private final LangAndDomain langAndDomain;

    /**
     * Creates a new instance of FetchTranslationsRequestDTO.
     *
     * @param headers The headers of the HTTP request.
     * @param langAndDomain The explicit language and domain information.
     */
    @JsonCreator
    public FetchTranslationsRequestDTO(
            @JsonProperty("headers") Map<String, String> headers,
            @JsonProperty("langAndDomain") LangAndDomain langAndDomain) {
        this.headers = headers;
        this.langAndDomain = langAndDomain;
    }

    /**
     * Gets the headers of the HTTP request.
     * @return The headers map. Can be null if explicit language and domain are provided.
     */
    public Map<String, String> getHeaders() {
        return headers;
    }

    /**
     * Gets the explicit language and domain information.
     * @return The LangAndDomain object. Can be null if headers are provided.
     */
    public LangAndDomain getLangAndDomain() {
        return langAndDomain;
    }

    /**
     * Validates the request to ensure that at least one of headers or langAndDomain is provided.
     * When langAndDomain is provided, it must be valid.
     * 
     * @return true if the request is valid, false otherwise.
     */
    public boolean isValid() {
        // Either headers or langAndDomain must be provided
        if (headers == null && langAndDomain == null) {
            return false;
        }
        
        // If langAndDomain is provided, it must be valid
        return langAndDomain == null || langAndDomain.isValid();
    }

    @Override
    public String toString() {
        return "FetchTranslationsRequestDTO{" +
                "headers=" + (headers != null ? "present" : "null") +
                ", langAndDomain=" + langAndDomain +
                '}';
    }

}