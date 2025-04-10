package org.entcore.broker.proxy;

import io.vertx.core.Future;
import org.entcore.broker.api.BrokerListener;
import org.entcore.broker.api.dto.i18n.FetchTranslationsRequestDTO;
import org.entcore.broker.api.dto.i18n.FetchTranslationsResponseDTO;

/**
 * This interface defines the methods that will be used to listen to events from the i18n broker.
 * It provides functionality to fetch translations by language and theme.
 */
public interface I18nBrokerListener {

  /**
   * This method is used to fetch translations for a specific language and theme.
   * @param request The request object containing the language and theme for which to fetch translations.
   * @return A response object containing a map of translation keys to their values in the requested language.
   */
  @BrokerListener(subject = "i18n.translations.fetch", proxy = true)
  Future<FetchTranslationsResponseDTO> fetchTranslations(FetchTranslationsRequestDTO request);
}