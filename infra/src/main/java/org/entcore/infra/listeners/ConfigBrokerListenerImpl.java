package org.entcore.infra.listeners;

import fr.wseduc.webutils.collections.SharedDataHelper;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.broker.api.dto.config.AuthLocationConfigDTO;
import org.entcore.broker.api.dto.config.GetRedirectConfigRequestDTO;
import org.entcore.broker.api.dto.config.RedirectConfigResponseDTO;
import org.entcore.broker.proxy.ConfigBrokerListener;
import org.entcore.broker.api.dto.config.RedirectConfigResponseDTO;

import java.util.ArrayList;
import java.util.List;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Broker listener for accessing shared server configuration.
 */
public class ConfigBrokerListenerImpl implements ConfigBrokerListener {

    private static final Logger log = LoggerFactory.getLogger(ConfigBrokerListenerImpl.class);

    @Override
    public Future<RedirectConfigResponseDTO> getRedirectConfig(GetRedirectConfigRequestDTO request) {
        final Promise<RedirectConfigResponseDTO> promise = Promise.promise();

        SharedDataHelper.getInstance().<String, Object>getLocalMulti(
                "server",
                "loginUri",
                "callbackParam",
                "authLocations",
                "skins"
        ).onSuccess(configMap -> {
            final String loginUri = (String) configMap.get("loginUri");
            final String callbackParam = (String) configMap.get("callbackParam");
            final String authLocationsStr = (String) configMap.get("authLocations");
            final JsonObject authLocationsJson = isNotBlank(authLocationsStr) ? new JsonObject(authLocationsStr) : null;
            final JsonObject skinsJson = (JsonObject) configMap.get("skins");

            List<AuthLocationConfigDTO> authLocations = null;
            if (authLocationsJson != null) {
                try {
                    authLocations = parseAuthLocations(authLocationsJson);
                } catch (Exception e) {
                    log.warn("Failed to parse authLocations JSON: {}", e.getMessage());
                }
            }

            final List<String> allowedHosts = new ArrayList<>();
            if (skinsJson != null) {
                try {
                    for (String host : skinsJson.fieldNames()) {
                        if (isNotBlank(host)) {
                            allowedHosts.add(host);
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse skins JSON for allowedHosts: {}", e.getMessage());
                }
            }

            final RedirectConfigResponseDTO response = new RedirectConfigResponseDTO(
                    loginUri,
                    callbackParam,
                    authLocations,
                    allowedHosts
            );
            promise.complete(response);
        }).onFailure(err -> {
            log.error("Failed to get redirect config from shared map", err);
            promise.fail(err);
        });

        return promise.future();
    }

    private List<AuthLocationConfigDTO> parseAuthLocations(JsonObject json) {
        final List<AuthLocationConfigDTO> result = new ArrayList<>();

        for (String key : json.fieldNames()) {
            final JsonObject location = json.getJsonObject(key);
            if (location != null) {
                result.add(new AuthLocationConfigDTO(
                        key,
                        location.getString("loginUri"),
                        location.getString("callbackParam")
                ));
            }
        }

        return result;
    }
}