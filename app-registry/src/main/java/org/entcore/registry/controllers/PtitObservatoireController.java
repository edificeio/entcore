package org.entcore.registry.controllers;

import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.BaseController;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserUtils;
import org.vertx.java.core.http.RouteMatcher;

import java.util.Map;

public class PtitObservatoireController extends BaseController {
    private HttpClient httpClient;
    private String ptitObservatoireUrl;
    private String ptitObservatoireApiKey;

    @Override
    public void init(Vertx vertx, JsonObject config, RouteMatcher rm, Map<String, fr.wseduc.webutils.security.SecuredAction> securedActions) {
        super.init(vertx, config, rm, securedActions);

        this.httpClient = vertx.createHttpClient(new HttpClientOptions());
        JsonObject ptitObservatoireConfiguration = config.getJsonObject("ptit-observatoire-widget-config");
        this.ptitObservatoireUrl = ptitObservatoireConfiguration.getString("url", "");
        this.ptitObservatoireApiKey = ptitObservatoireConfiguration.getString("api-key", "");
    }

    // Get the list of students related to a teacher.
    @Get("/ptitObservatoire/students")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getPtitObservatoireStudents(final HttpServerRequest request) {
        getIdLpo(request)
                .compose(idLpo -> {
                    String url = ptitObservatoireUrl + "/students?filter[teacher_id]=" + idLpo;

                    RequestOptions options = new RequestOptions()
                            .setMethod(HttpMethod.GET)
                            .setAbsoluteURI(url)
                            .addHeader("X-API-KEY", ptitObservatoireApiKey)
                            .addHeader("Content-Type", "application/json");

                    return httpClient.request(options)
                            .compose(HttpClientRequest::send)
                            .compose(response -> {
                                Promise<JsonArray> promise = Promise.promise();
                                response.bodyHandler(body -> {
                                    if (response.statusCode() == 200) {
                                        JsonObject json = body.toJsonObject();
                                        JsonArray data = json.getJsonArray("data", new JsonArray());
                                        promise.complete(data);
                                    } else {
                                        promise.fail("Error from API: " + response.statusCode());
                                    }
                                });
                                return promise.future();
                            });
                })
                .onSuccess(students -> renderJson(request, students))
                .onFailure(err -> renderError(request, null, 500, err.getMessage()));
    }

    // Get the list of categories related to a teacher.
    @Get("/ptitObservatoire/observations/categories")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getPtitObservatoireObservationsCategories(HttpServerRequest request) {
        getIdLpo(request)
                .compose(idLpo -> {
                    String url = ptitObservatoireUrl + "/observations/categories?filter[teacher_id]=" + idLpo;

                    RequestOptions options = new RequestOptions()
                            .setMethod(HttpMethod.GET)
                            .setAbsoluteURI(url)
                            .addHeader("X-API-KEY", ptitObservatoireApiKey)
                            .addHeader("Content-Type", "application/json");

                    return httpClient.request(options)
                            .compose(HttpClientRequest::send)
                            .compose(response -> {
                                Promise<JsonArray> promise = Promise.promise();
                                response.bodyHandler(body -> {
                                    if (response.statusCode() == 200) {
                                        JsonObject json = body.toJsonObject();
                                        JsonArray data = json.getJsonArray("data", new JsonArray());
                                        promise.complete(data);
                                    } else {
                                        promise.fail("Error from API: " + response.statusCode());
                                    }
                                });
                                return promise.future();
                            });
                })
                .onSuccess(categories -> renderJson(request, categories))
                .onFailure(err -> renderError(request, null, 500, err.getMessage()));
    }

    // Create a new observation.
    @Post("/ptitObservatoire/observations")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void createPtitObsevatoireObservation(HttpServerRequest request) {
        Future<Buffer> bodyFuture = request.body();
        Future<String> idLpoFuture = getIdLpo(request);

        // Wait for both futures to complete
        Future.all(bodyFuture, idLpoFuture)
                .compose(cf -> {
                    Buffer body = cf.resultAt(0);
                    String idLpo = cf.resultAt(1);

                    JsonObject json = body.toJsonObject();

                    // Modify the json object to include the teacher_id in the body
                    JsonObject attributes = json
                            .getJsonObject("data", new JsonObject())
                            .getJsonObject("attributes", new JsonObject());
                    attributes.put("teacher_id", idLpo);

                    RequestOptions options = new RequestOptions()
                            .setMethod(HttpMethod.POST)
                            .setAbsoluteURI(ptitObservatoireUrl + "/observations")
                            .addHeader("X-API-KEY", ptitObservatoireApiKey)
                            .addHeader("Content-Type", "application/json");

                    return httpClient.request(options)
                            .compose(req -> req.send(json.encode()));
                })
                .onSuccess(response -> {
                    response.bodyHandler(buffer -> {
                        JsonObject resJson = buffer.toJsonObject();
                        if (response.statusCode() == 201) {
                            renderJson(request, resJson);
                        } else {
                            renderError(request, resJson);
                        }
                    });
                })
                .onFailure(err -> renderError(request, null, 500, err.getMessage()));
    }

    // Get the ID of a teacher in the PtitObservatoire API.
    private Future<String> getIdLpo(HttpServerRequest request) {
        return UserUtils.getAuthenticatedUserInfos(eb, request)
                .compose(userInfos -> {
                    String userId = userInfos.getUserId();

                    String url = ptitObservatoireUrl + "/users?filter[external_id]=" + userId;

                    RequestOptions options = new RequestOptions()
                            .setMethod(HttpMethod.GET)
                            .setAbsoluteURI(url)
                            .addHeader("X-API-KEY", ptitObservatoireApiKey)
                            .addHeader("Content-Type", "application/json");

                    return httpClient.request(options)
                            .compose(HttpClientRequest::send)
                            .compose(response -> {
                                Promise<String> promise = Promise.promise();
                                response.bodyHandler(body -> {
                                    if (response.statusCode() != 200) {
                                        promise.fail("API error: " + response.statusCode());
                                        return;
                                    }

                                    JsonArray data = body.toJsonObject().getJsonArray("data");
                                    if (data == null || data.isEmpty()) {
                                        promise.fail("User not found");
                                    } else {
                                        String idLpo = data.getJsonObject(0).getString("id");
                                        promise.complete(idLpo);
                                    }
                                });
                                return promise.future();
                            });
                });
    }
}
