package org.entcore.registry.controllers;

import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.BaseController;
import io.vertx.core.Vertx;
import io.vertx.core.http.*;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
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

    @Get("/ptitObservatoire/students")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getPtitObservatoireStudents(final HttpServerRequest request) {

        httpClient.request(new RequestOptions()
                        .setMethod(HttpMethod.GET)
                        .setAbsoluteURI(ptitObservatoireUrl + "/students?filter[teacher_id]=1") // TODO Teacher ID (voir UserUtils)
                        .setHeaders(new HeadersMultiMap()
                                .add("X-API-KEY", ptitObservatoireApiKey)))
                .flatMap(HttpClientRequest::send)
                .onSuccess(response -> {
                    if (response.statusCode() == 200) {
                        response.bodyHandler(body -> {
                            JsonObject json = body.toJsonObject();
                            if (json.containsKey("data")) {
                                renderJson(request, json.getJsonArray("data"));
                            } else {
                                renderJson(request, new JsonArray());
                            }
                        });
                    } else {
                        response.bodyHandler(body -> {
                            JsonObject json = body.toJsonObject();
                            renderError(request, json);
                        });
                    }
                }).onFailure(err -> {
                    renderError(request, null, 500, err.getMessage());
                });
    }

    @Get("/ptitObservatoire/observations/categories")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getPtitObservatoireObservationsCategories(HttpServerRequest request) {
        httpClient.request(new RequestOptions()
                        .setMethod(HttpMethod.GET)
                        .setAbsoluteURI(ptitObservatoireUrl + "/observations/categories?filter[teacher_id]=1") // TODO Teacher ID (voir UserUtils   )
                        .setHeaders(new HeadersMultiMap()
                                .add("X-API-KEY", ptitObservatoireApiKey))
                )
                .flatMap(HttpClientRequest::send)
                .onSuccess(response -> {
                    if (response.statusCode() == 200) {
                        response.bodyHandler(body -> {
                            JsonObject json = body.toJsonObject();
                            if (json.containsKey("data")) {
                                renderJson(request, json.getJsonArray("data"));
                            } else {
                                renderJson(request, new JsonArray());
                            }
                        });
                    } else {
                        response.bodyHandler(body -> {
                            JsonObject json = body.toJsonObject();
                            renderError(request, json);
                        });
                    }
                })
                .onFailure(err -> {
                    renderError(request, null, 500, err.getMessage());
                });
    }

    @Post("/ptitObservatoire/observations")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void createPtitObsevatoireObservation(HttpServerRequest request) {
        request.bodyHandler(requestBody -> {
            RequestOptions requestOptions = new RequestOptions()
                    .setMethod(HttpMethod.POST)
                    .setAbsoluteURI(ptitObservatoireUrl + "/observations")
                    .setHeaders(new HeadersMultiMap()
                            .add("X-API-KEY", ptitObservatoireApiKey)
                            .add("Content-Type", "application/json")
                    );

            httpClient.request(requestOptions)
                    .flatMap(req -> {
                        return req.send(requestBody.toString());
                    })
                    .onSuccess(response -> {
                        if (response.statusCode() == 201) {
                            response.bodyHandler(body -> {
                                JsonObject json = body.toJsonObject();
                                renderJson(request, json);
                            });
                        } else {
                            response.bodyHandler(body -> {
                                JsonObject json = body.toJsonObject();
                                renderError(request, json);
                            });
                        }
                    })
                    .onFailure(err -> {
                        renderError(request, null, 500, err.getMessage());
                    });
        });
    }
}
