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
                    promise.fail("[WebGerest] - " + err.getMessage());
                });

        return promise.future();
    }

    private Future<JsonObject> fetchMenuApi(String accessToken, String webServiceUrl, String uai, String date) {
        Promise<JsonObject> promise = Promise.promise();
        String api = webServiceUrl + "/menus?rne=" + uai + "&date_menu=" + date + "&service=2";
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
                                promise.complete(new JsonObject().put("menu", menu));
                            } else {
                                log.error("[WebGerest] - Menu key not found");
                                promise.fail("[WebGerest] - Menu key not found");
                            }
                        });
                    } else {
                        log.error("[WebGerest] - Menu not found");
                        promise.fail("[WebGerest] - Menu not found");
                    }
                })
                .onFailure(err -> {
                    log.error("[WebGerest] - " + err.getMessage());
                    promise.fail("[WebGerest] - " + err.getMessage());
                });

        return promise.future();
    }
}
