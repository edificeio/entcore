package org.entcore.registry.services.impl;

import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
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
        //0770000X
        Promise<String> promise = Promise.promise();

        httpClient.request(HttpMethod.GET, 443, url, uri)
                .compose(req -> req.send())
                .compose(HttpClientResponse::body)
                .onSuccess(body -> {
                    JsonObject resp = body.toJsonObject();
                    String webServiceUrl = resp.getString("contenu");
                    if (webServiceUrl != null && !webServiceUrl.isEmpty()) {
                        log.info("webService response: " + webServiceUrl);
                        promise.complete(webServiceUrl);
                    } else {
                        promise.fail("WebService Url not found");
                    }
                })
                .onFailure(promise::fail);

        return promise.future();
    }

    private Future<String> getAccessToken(String webService, String client_id, String client_secret) {
        log.info("[WebGerest] - fetching access Token for webService: " + webService);
        Promise<String> promise = Promise.promise();
        String uri = "/auth?client_id=" + client_id + "&client_secret=" + client_secret;

        httpClient.request(HttpMethod.GET, 443, webService, uri)
                .compose(req -> req.send())
                .compose(HttpClientResponse::body)
                .onSuccess(body -> {
                    JsonObject resp = body.toJsonObject();
                    String accessToken = resp.getString("token");
                    if (accessToken != null && !accessToken.isEmpty()) {
                        promise.complete(accessToken);
                    } else {
                        promise.fail("[WebGerest] - Access Token not found");
                    }
//                }).onFailure(promise::fail);
                }).onFailure(err -> {
                    log.error("[WebGerest] - Failed to fetch access token:" + err.getMessage());
                    promise.complete("mock_access_token");
                });
        return promise.future();
    }

    private Future<JsonObject> fetchMenuApi(String accessToken, String webServiceUrl, String uai) {
        Promise<JsonObject> promise = Promise.promise();
        String uri = "/menus?rne =" + uai + "&date_menu=" + LocalDate.now() + "&service=2";

        httpClient.request(HttpMethod.GET, 443, webServiceUrl, uri)
                .compose(req -> req.putHeader("Authorization", accessToken)
                        .send())
                .compose(HttpClientResponse::body)
                .onSuccess(body -> {
                    JsonObject menu = body.toJsonObject().getJsonObject("contenu");
                    if (menu != null && !menu.isEmpty()) {
                        promise.complete(menu);
                    } else {
                        promise.fail("Menu could not be retreived");
                    }
                })
                .onFailure(err -> {
                    log.error("Failed to fetch menu: " + err.getMessage());
                    promise.complete(getMockMenu());  // Return mock menu on request failure
                });
        ;
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
