package org.entcore.portal.listeners;

import fr.wseduc.webutils.I18n;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.entcore.broker.api.dto.i18n.FetchTranslationsRequestDTO;
import org.entcore.broker.api.dto.i18n.FetchTranslationsResponseDTO;
import org.entcore.broker.proxy.I18nBrokerListener;
import org.entcore.common.http.request.JsonHttpServerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of the I18nBrokerListener interface.
 * This class handles internationalization operations received through the message broker.
 */
public class I18nBrokerListenerImpl implements I18nBrokerListener {

    private static final Logger log = LoggerFactory.getLogger(I18nBrokerListenerImpl.class);
    private final I18n i18n;

    /**
     * Constructor for I18nBrokerListenerImpl.
     */
    public I18nBrokerListenerImpl() {
        this(I18n.getInstance());
    }

    /**
     * Constructor for I18nBrokerListenerImpl with I18n dependency.
     *
     * @param i18n The I18n instance
     */
    public I18nBrokerListenerImpl(I18n i18n) {
        this.i18n = i18n;
    }

    /**
     * Fetches translations based on the provided request.
     * The request can either contain HTTP headers or explicit language and domain information.
     *
     * @param request The request containing either headers or language and domain
     * @return A Future containing the response with translations
     */
    @Override
    public Future<FetchTranslationsResponseDTO> fetchTranslations(FetchTranslationsRequestDTO request) {
        Promise<FetchTranslationsResponseDTO> promise = Promise.promise();

        // Validate the request
        if (request == null || !request.isValid()) {
            log.error("Invalid fetchTranslations request: request is null or invalid {}", request);
            return Future.failedFuture("i18n.parameters.invalid");
        }

        try {
            final JsonObject translations =  new JsonObject();

            // Use headers or explicit language and domain
            if (request.getHeaders() != null) {
                // Create a simulated HttpServerRequest with the provided headers
                final MultiMap headers = MultiMap.caseInsensitiveMultiMap();
                for (Map.Entry<String, String> entry : request.getHeaders().entrySet()) {
                    headers.add(entry.getKey(), entry.getValue());
                }

                final HttpServerRequest httpRequest = new JsonHttpServerRequest(new JsonObject(), headers);
                // Use I18n to load translations based on headers
                translations.mergeIn(i18n.load(httpRequest));
            } else {
                // Use explicit language and domain
                final FetchTranslationsRequestDTO.LangAndDomain langAndDomain = request.getLangAndDomain();
                // Use I18n to load translations based on language and domain
                //noinspection deprecation
                translations.mergeIn(langAndDomain.getDomain() ==  null ?
                        i18n.load(langAndDomain.getLang()) :
                        i18n.load(langAndDomain.getLang(), langAndDomain.getDomain())
                );
            }

            // Convert JsonObject to Map<String, String>
            final Map<String, String> translationsMap = new HashMap<>();
            for (String key : translations.fieldNames()) {
                Object value = translations.getValue(key);
                // Only include string values
                if (value instanceof String) {
                    translationsMap.put(key, (String) value);
                }
            }

            // Create and complete the response
            final FetchTranslationsResponseDTO response = new FetchTranslationsResponseDTO(translationsMap);
            promise.complete(response);
        } catch (Exception e) {
            log.error("Error fetching translations", e);
            promise.fail("i18n.fetch.error");
        }

        return promise.future();
    }
}