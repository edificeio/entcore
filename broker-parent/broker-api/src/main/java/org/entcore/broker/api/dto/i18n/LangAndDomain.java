package org.entcore.broker.api.dto.i18n;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.StringUtils;
import java.beans.Transient;

/**
 * This class represents language and domain information.
 */
public class LangAndDomain {
  /**
   * The language code for which to fetch translations.
   * This should be a standard language code (e.g., "en", "fr", "de").
   */
  private final String lang;

  /**
   * The domain or theme name for which to fetch translations.
   */
  private final String domain;

  /**
   * Creates a new instance of LangAndDomain.
   *
   * @param lang The language code for which to fetch translations.
   * @param domain The domain or theme name for which to fetch translations.
   */
  @JsonCreator
  public LangAndDomain(
    @JsonProperty("lang") String lang,
    @JsonProperty("domain") String domain) {
    this.lang = lang;
    this.domain = domain;
  }

  /**
   * Gets the language code for which to fetch translations.
   * @return The language code (e.g., "en", "fr", "de").
   */
  public String getLang() {
    return lang;
  }

  /**
   * Gets the domain or theme name for which to fetch translations.
   * @return The domain or theme name.
   */
  public String getDomain() {
    return domain;
  }

  /**
   * Checks if the object is valid by verifying that lang is provided.
   * @return true if both lang is not blank, false otherwise.
   */
  @Transient()
  public boolean isValid() {
    return !StringUtils.isBlank(lang);
  }

  @Override
  public String toString() {
    return "LangAndDomain{" +
      "lang='" + lang + '\'' +
      ", domain='" + domain + '\'' +
      '}';
  }
}
