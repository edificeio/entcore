package org.entcore.registry.controllers;

import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.BaseController;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.registry.services.MongoWidgetExternalCacheService;
import org.entcore.registry.services.WebGerestService;
import org.entcore.registry.services.impl.DefaultMongoWidgetExternalCacheService;
import org.entcore.registry.services.impl.DefaultWebGerestService;
import org.vertx.java.core.http.RouteMatcher;

import java.util.Map;

public class WebGerestController extends BaseController {
    private JsonObject webGerestConfig;
    private WebGerestService webGerestService;
    private MongoWidgetExternalCacheService cacheService;

    private static final String  WEBGREST_PREFIX_CACHE = "WEBGEREST";


    @Override
    public void init(Vertx vertx, JsonObject config, RouteMatcher rm, Map securedActions) {

        this.cacheService = new DefaultMongoWidgetExternalCacheService();
        this.webGerestService = new DefaultWebGerestService(
                vertx.createHttpClient(new HttpClientOptions()));
        this.webGerestConfig = config.getJsonObject("webGerest-config");
        super.init(vertx, config, rm, securedActions);
    }

    @Get("/:uai/cantine/menu")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void fetchMenu(final HttpServerRequest httpServerRequest) {

        String uai = httpServerRequest.params().get("uai");
        String date = httpServerRequest.params().get("date");

        if (checkUaiAndDate(httpServerRequest, uai, date)) return;

        String cacheId = WEBGREST_PREFIX_CACHE.concat(uai).concat("-").concat(date);

        cacheService.get(cacheId, result -> {
            if (result.isRight()) {
                if (result.right().getValue() != null && result.right().getValue().containsKey("cache")) {
                    JsonArray menuArray = new JsonArray(result.right().getValue().getString("cache"));
                    JsonObject responseJson = new JsonObject().put("menu", menuArray);
                    renderJson(httpServerRequest, responseJson);
                } else {
                    webGerestService.getMenu(httpServerRequest, uai, date, webGerestConfig, resp -> {
                        if (resp.isRight()) {
                            JsonObject menu = resp.right().getValue();
                            cacheService.put(cacheId, menu.getJsonArray("menu").encode(),
                                    webGerestConfig.getInteger("webGerest-cache-ttl"));
                            renderJson(httpServerRequest, menu);

                        } else {
                            log.error("[WebGerest] - Failed to fetch menu: " + resp.left().getValue());
                            renderError(httpServerRequest,null, 400, resp.left().getValue());
                        }
                    });
                }
            } else {
                log.error("[WebGerest] - MongoDB unavailable");
                renderError(httpServerRequest);
            }
        });
    }

    private static boolean checkUaiAndDate(HttpServerRequest httpServerRequest, String uai, String date) {
        if (uai == null || uai.isEmpty()) {
            renderError(httpServerRequest, null, 400, "Missing UAI parameter");
            return true;
        }
        if (date == null || date.isEmpty()) {
            renderError(httpServerRequest, null, 400, "Missing Date parameter");
            return true;
        }
        return false;
    }
}