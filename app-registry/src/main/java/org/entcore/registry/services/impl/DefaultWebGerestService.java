package org.entcore.registry.services.impl;

import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.http.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.registry.services.WebGerestService;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

public class DefaultWebGerestService implements WebGerestService {

    private HttpClient httpClient;

    private static final Logger log = LoggerFactory.getLogger(DefaultLibraryService.class);

    public DefaultWebGerestService(HttpClient client) {
        this.httpClient = client;
    }

    @Override
    public void getMenu(HttpServerRequest httpServerRequest, String uai, String date, JsonObject config, Handler<Either<String, JsonObject>> eitherHandler) {

        this.getStructureWebservice(uai, config.getString("webGerest-url"))
                .compose(webServiceurl -> getAccessToken(webServiceurl,
                        config.getString("webGerest-client-id"),
                        config.getString("webGerest-client-secret"))
                        .compose(accessToken -> fetchMenuApi(accessToken, webServiceurl, uai, date)))
                .onSuccess(menu -> eitherHandler.handle(new Either.Right<>(menu)))
                .onFailure(err ->
                        eitherHandler.handle(new Either.Left<>(err.getMessage())));
    }

    private Future<String> getStructureWebservice(String uai, String url) {
        String uri = "/url?RNE=" + uai;
        String apiUrl = url.concat(uri);

        Promise<String> promise = Promise.promise();
        httpClient.request(new RequestOptions()
                        .setMethod(HttpMethod.GET)
                        .setAbsoluteURI(apiUrl))
                .flatMap(HttpClientRequest::send)
                .onSuccess(response -> {
                    if (response.statusCode() == 200) {
                        response.bodyHandler(body -> {
                            JsonObject json = body.toJsonObject();
                            if (json.containsKey("contenu")) {
                                promise.complete(json.getString("contenu"));
                            } else {
                                log.error("[WebGerest] - WebService URL not found");
                                promise.fail("[WebGerest] - WebService URL not found");
                            }
                        });
                    } else {
                        log.error("[WebGerest] - Cant get Webservice failed");
                        promise.fail("[WebGerest] - Cant get Webservice failed");
                    }
                })
                .onFailure(err -> {
                    log.error("[WebGerest] - " + err.getMessage());
                    promise.fail(err.getMessage());
                });
        return promise.future();
    }

    private Future<String> getAccessToken(String webService, String client_id, String client_secret) {
        Promise<String> promise = Promise.promise();
        String api = webService + "/auth?client_id=" + client_id + "&client_secret=" + client_secret;

        this.httpClient.request(new RequestOptions()
                        .setMethod(HttpMethod.GET)
                        .setAbsoluteURI(api))
                .flatMap(HttpClientRequest::send)
                .onSuccess(response -> {
                    if (response.statusCode() == 200) {
                        response.bodyHandler(body -> {
                            JsonObject json = body.toJsonObject();
                            if (json.containsKey("token")) {
                                promise.complete(json.getString("token"));
                            } else {
                                log.error("[WebGerest] - Token not found");
                                promise.fail("[WebGerest] - Token not found");
                            }
                        });
                    } else {
                        log.error("Cant get access token Server Unreachable");
                        promise.fail("Cant get access token Server Unreachable");
                    }
                })
                .onFailure(err -> {
                    log.error("[WebGerest] - " + err.getMessage());
                    promise.fail(err.getMessage());
                });

        return promise.future();
    }

    private Future<JsonObject> fetchMenuApi(String accessToken, String webServiceUrl, String uai, String date) {
        // First fetch lunch menu (service=2) - this is required
        Future<JsonArray> lunchMenuFuture = fetchMenuByService(accessToken, webServiceUrl, uai, date, 2);
        
        // Then fetch dinner menu (service=4) - this is optional
        Future<JsonArray> dinnerMenuFuture = fetchMenuByService(accessToken, webServiceUrl, uai, date, 4);

        // Wait for lunch menu first (required), then check dinner menu
        return lunchMenuFuture
                .compose(lunchMenu -> {
                    if (lunchMenu == null) {
                        return Future.failedFuture("[WebGerest] - Menu key not found");
                    }
                    
                    JsonObject response = new JsonObject().put("lunchMenu", lunchMenu);
                    
                    // Check dinner menu result (non-blocking, already fetched)
                    return dinnerMenuFuture
                            .map(dinnerMenu -> {
                                boolean dinnerAvailable = dinnerMenu != null && dinnerMenu.size() > 0;
                                response.put("dinnerAvailable", dinnerAvailable);
                                
                                if (dinnerAvailable) {
                                    response.put("dinnerMenu", dinnerMenu);
                                }
                                
                                return response;
                            })
                            .recover(err -> {
                                // If dinner fetch failed, just set dinnerAvailable to false
                                response.put("dinnerAvailable", false);
                                return Future.succeededFuture(response);
                            });
                });
    }

    private Future<JsonArray> fetchMenuByService(String accessToken, String webServiceUrl, String uai, String date, int service) {
        Promise<JsonArray> promise = Promise.promise();
        String api = webServiceUrl + "/menus?rne=" + uai + "&date_menu=" + date + "&service=" + service;
        MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        headers.add("Authorization", accessToken);

        httpClient.request(new RequestOptions()
                        .setMethod(HttpMethod.GET)
                        .setAbsoluteURI(api)
                        .setHeaders(headers))
                .flatMap(HttpClientRequest::send)
                .onSuccess(response -> {
                    if (response.statusCode() == 200) {
                        response.bodyHandler(body -> {
                            JsonObject json = body.toJsonObject();
                            if (json.containsKey("contenu")) {
                                JsonArray menu = json.getJsonArray("contenu");
                                // Empty response structure: {"error": "0", "nbObjet": 0, "contenu": []}
                                if (menu != null && menu.size() > 0) {
                                    promise.complete(menu);
                                } else {
                                    // {"error": "0", "nbObjet": 0, "contenu": []} means no data available
                                    promise.complete(null);
                                }
                            } else {
                                // If contenu key doesn't exist, treat as no data
                                promise.complete(null);
                            }
                        });
                    } else {
                        // If request fails, treat as no data available
                        if (service == 2) {
                            // For lunch (service=2), this is an error
                            log.error("[WebGerest] - Menu not found for service=" + service);
                            promise.fail("[WebGerest] - Menu not found");
                        } else {
                            // For dinner (service=4), this is acceptable (service may not be available)
                            log.warn("[WebGerest] - Menu not found for service=" + service);
                            promise.complete(null);
                        }
                    }
                })
                .onFailure(err -> {
                    if (service == 2) {
                        // For lunch (service=2), this is an error
                        log.error("[WebGerest] - " + err.getMessage());
                        promise.fail(err.getMessage());
                    } else {
                        // For dinner (service=4), this is acceptable (service may not be available)
                        log.warn("[WebGerest] - Error fetching menu for service=" + service + ": " + err.getMessage());
                        promise.complete(null);
                    }
                });

        return promise.future();
    }

    @Override
    public boolean validateDateLimit(String date, JsonObject config) {
        if (config == null) {
            return true;
        }

        Integer maxDaysAhead = config.getInteger("webGerest-fetch-max-days-ahead");
        if (maxDaysAhead == null) {
            return true;
        }

        try {
            // Frontend sends dates in YYYY-MM-DD format
            LocalDate requestDate = LocalDate.parse(date);
            LocalDate today = LocalDate.now();
            long diffInDays = ChronoUnit.DAYS.between(today, requestDate);

            if (diffInDays > maxDaysAhead) {
                return false;
            }
        } catch (DateTimeParseException e) {
            log.error("[WebGerest] - Error parsing date: " + date);
            return true;
        }

        return true;
    }
}
