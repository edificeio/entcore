package org.entcore.broker.proxy;

import io.vertx.core.Future;
import org.entcore.broker.api.BrokerListener;
import org.entcore.broker.api.dto.config.GetRedirectConfigRequestDTO;
import org.entcore.broker.api.dto.config.RedirectConfigResponseDTO;

/**
 * Broker listener interface for accessing shared server configuration.
 */
public interface ConfigBrokerListener {

    /**
     * Retrieves the redirect configuration from the shared server map.
     * This includes loginUri, callbackParam, and per-host authLocations.
     *
     * @param request - Empty request DTO (placeholder for future parameters)
     * @return Future containing the redirect configuration
     */
    @BrokerListener(subject = "config.redirect", proxy = true)
    Future<RedirectConfigResponseDTO> getRedirectConfig(GetRedirectConfigRequestDTO request);
}