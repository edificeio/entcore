package org.entcore.registry.controllers;

import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.BaseController;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.utils.StringUtils;
import org.entcore.registry.services.MongoWidgetExternalCacheService;
import org.entcore.registry.services.ScreenTimeService;
import org.entcore.registry.services.impl.DefaultMongoWidgetExternalCacheService;
import org.entcore.registry.services.impl.DefaultScreenTimeService;
import org.vertx.java.core.http.RouteMatcher;

import java.io.UnsupportedEncodingException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class ScreenTimeController extends BaseController {

    private JsonObject authConfig;
    private JsonObject apiConfig;
    private ScreenTimeService screenTimeService;
    private MongoWidgetExternalCacheService cacheService;
    private Neo4j neo4j = Neo4j.getInstance();

    private static final String  SCREENTIME_PREFIX_CACHE = "SCREENTIME";

    @Override
    public void init (Vertx vertx, JsonObject config, RouteMatcher rm, Map securedActions) {
        this.cacheService = new DefaultMongoWidgetExternalCacheService();
        this.screenTimeService = new DefaultScreenTimeService(vertx);
        this.authConfig = config.getJsonObject("screen-time-config").getJsonObject("auth");
        this.apiConfig = config.getJsonObject("screen-time-config").getJsonObject("api");
        super.init(vertx, config, rm, securedActions);
    }

    @Get("/screen-time/:id/daily")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void fetchDailyScreenTime(final HttpServerRequest httpServerRequest) {
        final String userId = httpServerRequest.getParam("id");
        final LocalDate date;
        try {
            date = LocalDate.parse(httpServerRequest.getParam("date"));
        } catch (Exception e) {
            log.error("Invalid date format", e);
            renderError(httpServerRequest, null, 400, "Invalid date format");
            return;
        }
        final String accessTokenCacheId = SCREENTIME_PREFIX_CACHE.concat("-ACCESS-TOKEN");

        final String query = "MATCH (u:User {id: {id}}) return u.externalId";
        final JsonObject params = new JsonObject().put("id", userId);

        neo4j.execute(query, params, event -> {
            String finalEntPersonJointure = userId;
            JsonArray r = event.body().getJsonArray("result");
            if ("ok".equals(event.body().getString("status")) && r != null && r.size() == 1) {
                String extId = r.getJsonObject(0).getString("u.externalId");
                if (extId != null && !extId.isEmpty()) {
                    finalEntPersonJointure = extId;
                }
            }

            final List<String> lExtId = StringUtils.split(finalEntPersonJointure, "-");
            final String entPersonJointure = finalEntPersonJointure;

            if (!lExtId.isEmpty() && lExtId.size() >= 5) {
                cacheService.get(accessTokenCacheId, result -> {
                    if (result.right().getValue() != null && result.right().getValue().containsKey("cache")) {
                        String accessToken = result.right().getValue().getString("cache");
                        try {
                            screenTimeService.getDailyScreenTime(httpServerRequest, accessToken,
                                                                 entPersonJointure, date, apiConfig, resp -> {
                                        if(resp.isRight()) {
                                            JsonObject data = resp.right().getValue();
                                            renderJson(httpServerRequest, data);
                                        } else {
                                            JsonObject error = resp.left().getValue();
                                            int statusCode = error.getInteger("statusCode", 400);
                                            String message = error.getString("message", "Unknown error");
                                            log.error("screen time - failed to fetch data: " + message);
                                            renderError(httpServerRequest, null, statusCode, message);
                                        }
                                    });
                        } catch (UnsupportedEncodingException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        try {
                            screenTimeService.getAccessToken(authConfig)
                                    .onSuccess(accessTokenObject -> {
                                        String accessToken = accessTokenObject.getString("access_token");
                                        Integer ttl = accessTokenObject.getInteger("expires_in");
                                        cacheService.put(accessTokenCacheId, accessToken, ttl);

                                        try {
                                            screenTimeService.getDailyScreenTime(httpServerRequest, accessToken,
                                                                                 entPersonJointure, date, apiConfig, resp -> {
                                                        if (resp.isRight()) {
                                                            JsonObject data = resp.right().getValue();
                                                            renderJson(httpServerRequest, data);
                                                        } else {
                                                            JsonObject error = resp.left().getValue();
                                                            int statusCode = error.getInteger("statusCode", 400);
                                                            String message = error.getString("message", "Unknown error");
                                                            log.error("screen time - failed to fetch data: " + message);
                                                            renderError(httpServerRequest, null, statusCode, message);
                                                        }
                                                    });
                                        } catch (UnsupportedEncodingException e) {
                                            throw new RuntimeException(e);
                                        }
                                    })
                                    .onFailure(err -> {
                                        log.error("screen time - failed to get access token", err);
                                        renderError(httpServerRequest, null, 500, "Unable to retrieve access token");
                                    });
                        } catch (UnsupportedEncodingException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            } else {
                log.warn("EntPersonJointure does not meet split criteria: " + entPersonJointure);
                renderError(httpServerRequest, null, 400, "Invalid user identifier format");
            }
        });
    }

    @Get("/screen-time/:id/weekly")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void fetchWeeklyScreenTime(final HttpServerRequest httpServerRequest) {
        final String userId = httpServerRequest.getParam("id");
        final LocalDate startDate;
        final LocalDate endDate;
        try {
            startDate = LocalDate.parse(httpServerRequest.getParam("startDate"));
            endDate = LocalDate.parse(httpServerRequest.getParam("endDate"));
        } catch (Exception e) {
            log.error("Invalid date format", e);
            renderError(httpServerRequest, null, 400, "Invalid startDate or endDate format");
            return;
        }
        final String accessTokenCacheId = SCREENTIME_PREFIX_CACHE.concat("-ACCESS-TOKEN");
        final String query = "MATCH (u:User {id: {id}}) return u.externalId";
        final JsonObject params = new JsonObject().put("id", userId);
        neo4j.execute(query, params, event -> {
            String finalEntPersonJointure = userId;
            JsonArray r = event.body().getJsonArray("result");
            if ("ok".equals(event.body().getString("status")) && r != null && r.size() == 1) {
                String extId = r.getJsonObject(0).getString("u.externalId");
                if (extId != null && !extId.isEmpty()) {
                    finalEntPersonJointure = extId;
                }
            }
            final List<String> lExtId = StringUtils.split(finalEntPersonJointure, "-");
            final String entPersonJointure = finalEntPersonJointure;
            if (!lExtId.isEmpty() && lExtId.size() >= 5) {
                cacheService.get(accessTokenCacheId, result -> {
                    if (result.right().getValue() != null && result.right().getValue().containsKey("cache")) {
                        String accessToken = result.right().getValue().getString("cache");
                        try {
                            screenTimeService.getWeeklyScreenTime(httpServerRequest, accessToken, entPersonJointure, startDate, endDate, apiConfig, resp -> {
                                if (resp.isRight()) {
                                    JsonObject data = resp.right().getValue();
                                    renderJson(httpServerRequest, data);
                                } else {
                                    JsonObject error = resp.left().getValue();
                                    int statusCode = error.getInteger("statusCode", 400);
                                    String message = error.getString("message", "Unknown error");
                                    log.error("screen time - failed to fetch data: " + message);
                                    renderError(httpServerRequest, null, statusCode, message);
                                }
                            });
                        } catch (UnsupportedEncodingException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        try {
                            screenTimeService.getAccessToken(authConfig)
                                    .onSuccess(accessTokenObject -> {
                                        String accessToken = accessTokenObject.getString("access_token");
                                        Integer ttl = accessTokenObject.getInteger("expires_in");
                                        cacheService.put(accessTokenCacheId, accessToken, ttl);
                                        try {
                                            screenTimeService.getWeeklyScreenTime(httpServerRequest, accessToken, entPersonJointure, startDate, endDate, apiConfig, resp -> {
                                                if (resp.isRight()) {
                                                    JsonObject data = resp.right().getValue();
                                                    renderJson(httpServerRequest, data);
                                                } else {
                                                    JsonObject error = resp.left().getValue();
                                                    int statusCode = error.getInteger("statusCode", 400);
                                                    String message = error.getString("message", "Unknown error");
                                                    log.error("screen time - failed to fetch data: " + message);
                                                    renderError(httpServerRequest, null, statusCode, message);
                                                }
                                            });
                                        } catch (UnsupportedEncodingException e) {
                                            throw new RuntimeException(e);
                                        }
                                    })
                                    .onFailure(err -> {
                                        log.error("screen time - failed to get access token", err);
                                        renderError(httpServerRequest, null, 500, "Unable to retrieve access token");
                                    });
                        } catch (UnsupportedEncodingException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            } else {
                log.warn("EntPersonJointure does not meet split criteria: " + entPersonJointure);
                renderError(httpServerRequest, null, 400, "Invalid user identifier format");
            }
        });
    }
}
