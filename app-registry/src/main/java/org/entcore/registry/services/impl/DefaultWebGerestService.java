package org.entcore.registry.services.impl;

import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.registry.services.WebGerestService;

import java.time.LocalDate;

public class DefaultWebGerestService implements WebGerestService {

    private HttpClient httpClient;

    private static final Logger log = LoggerFactory.getLogger(DefaultLibraryService.class);

    public DefaultWebGerestService(HttpClient client) {
        this.httpClient = client;
    }

    @Override
    public void getMenu(String uai, JsonObject config, Handler<Either<String, JsonObject>> eitherHandler) {
        // Fetch WebService
        log.info("[WebGerest] - entered service");
        this.getStructureWebservice(uai, config.getString("webgerest-url"))
                .compose(webServiceurl -> getAccessToken(webServiceurl,
                        config.getString("webgerest_client_id"),
                        config.getString("webGerest_client_secret"))
                        .compose(accessToken -> fetchMenuApi(accessToken, webServiceurl, uai)))
                .onSuccess(menu -> eitherHandler.handle(new Either.Right<>(menu)))
                .onFailure(err ->
                        eitherHandler.handle(new Either.Left<>(err.getMessage())));
    }

    private Future<String> getStructureWebservice(String uai, String url) {
        log.info("[WebGerest] - Getting WebService for UAI: " + uai);
        String uri = "/url?RNE=" + uai;
        String apiUrl = url.concat(uri);
        //0770000X
        Promise<String> promise = Promise.promise();
        httpClient.request(new RequestOptions()
                        .setMethod(HttpMethod.GET)
                        .setAbsoluteURI(apiUrl))
                .compose(HttpClientRequest::send)
                .compose(response -> response.body().compose(buffer -> {
                    try {
                        JsonObject jsonObject = buffer.toJsonObject();
                        String webServiceUrl = jsonObject.getString("contenu");

                        if (webServiceUrl == null || webServiceUrl.isEmpty()) {
                            log.error("[WebGerest] - WebService URL not found in response");
                            return Future.failedFuture("[WebGerest] - WebService URL not found");
                        }

                        log.info("[WebGerest] - WebService response: {}", webServiceUrl);
                        return Future.succeededFuture(webServiceUrl);
                    } catch (Exception e) {
                        log.error("[WebGerest] - Failed to parse JSON response", e);
                        return Future.failedFuture("[WebGerest] - Invalid JSON response");
                    }
                }))
                .onSuccess(promise::complete)
                .onFailure(error -> {
                    log.error("[WebGerest] - Failed to retrieve WebService URL", error);
                    promise.fail(error);
                });

        return promise.future();
    }

    private Future<String> getAccessToken(String webService, String client_id, String client_secret) {
        log.info("[WebGerest] - fetching access Token for webService: " + webService);
        Promise<String> promise = Promise.promise();
        String api = webService + "/auth?client_id=" + client_id + "&client_secret=" + client_secret;

        httpClient.request(new RequestOptions()
                        .setMethod(HttpMethod.GET)
                        .setAbsoluteURI(api))
                .compose(HttpClientRequest::send)
                .compose(response -> response.body().compose(buffer -> {
                    try {
                        JsonObject jsonObject = buffer.toJsonObject();
                        String token = jsonObject.getString("token");

                        if (token == null || token.isEmpty()) {
                            log.error("[WebGerest] - Token not found in response");
                            return Future.failedFuture("[WebGerest] - Token not found");
                        }

                        log.info("[WebGerest] - Token retrieved successfully.");
                        return Future.succeededFuture(token);
                    } catch (Exception e) {
                        log.error("[WebGerest] - Failed to parse JSON response", e);
                        return Future.failedFuture("[WebGerest] - Invalid JSON response");
                    }
                })).onSuccess(promise::complete)
                .onFailure(error -> log.error("[WebGerest] - Failed to retrieve access token", error));

        return promise.future();
    }

    private Future<JsonObject> fetchMenuApi(String accessToken, String webServiceUrl, String uai) {
        Promise<JsonObject> promise = Promise.promise();
        String api = webServiceUrl + "/menus?rne=" + uai + "&date_menu=" + LocalDate.now() + "&service=2";
        MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        headers.add("Authorization", accessToken);
        httpClient.request(new RequestOptions()
                        .setMethod(HttpMethod.GET)
                        .setAbsoluteURI(api)
                        .setHeaders(headers))
                .compose(HttpClientRequest::send)
                .compose(response ->
                        response.body().compose(buffer -> {
                            try {
                                JsonObject jsonObject = buffer.toJsonObject();
                                JsonArray menu = jsonObject.getJsonArray("contenu");

                                if (menu == null || menu.isEmpty()) {
                                    log.error("[WebGerest] - Menu not found in response");
                                    return Future.failedFuture("[WebGerest] - Menu not found");
                                }

                                log.info("[WebGerest] - Menu retrieved successfully.");

                                // Wrap the menu in a JsonObject if needed
                                JsonObject result = new JsonObject();
                                result.put("menu", menu);

                                return Future.succeededFuture(result);
                            } catch (Exception e) {
                                log.error("[WebGerest] - Failed to parse JSON response", e);
                                return Future.failedFuture("[WebGerest] - Invalid JSON response");
                            }
                        }))
                .onSuccess(promise::complete)
                .onFailure(error -> {
                    log.error("[WebGerest] - Failed to retrieve menu", error);
                    promise.fail(error); // Ensure promise fails if there's an error
                });

        return promise.future();
    }


    /**
     * Returns a mock JSON menu.
     */
    private JsonObject getMockMenu() {
        return new JsonObject()
                .put("contenu", new JsonObject()
                        .put("menu", "Mocked Menu")
                        .put("date", LocalDate.now().toString())
                        .put("items", new JsonArray()
                                .add(new JsonObject().put("name", "Mock petit dejeuner").put("type", "petit dejeuner").put("local", true).put("bio", true))
                                .add(new JsonObject().put("name", "Mock dejeuner").put("type", "dejeuner").put("local", false).put("bio", true))
                                .add(new JsonObject().put("name", "Mock gouter").put("type", "gouter").put("local", true).put("bio", false))
                                .add(new JsonObject().put("name", "Mock diner").put("type", "diner").put("local", true).put("bio", false))
                        )
                );
    }
}
