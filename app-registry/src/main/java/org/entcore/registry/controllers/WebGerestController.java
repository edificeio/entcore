package org.entcore.registry.controllers;

import fr.wseduc.rs.Get;
import fr.wseduc.webutils.http.BaseController;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.entcore.registry.services.WebGerestService;
import org.entcore.registry.services.WidgetExternalCacheService;
import org.entcore.registry.services.impl.DefaultWebGerestService;
import org.entcore.registry.services.impl.DefaultWidgetExternalCacheService;
import org.vertx.java.core.http.RouteMatcher;

import java.util.Map;

import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;

public class WebGerestController extends BaseController {
    private JsonObject config;
    private WebGerestService webGerestService;
    private WidgetExternalCacheService externalCacheService;

    @Override
    public void init(Vertx vertx, JsonObject config, RouteMatcher rm, Map securedActions) {

        this.webGerestService = new DefaultWebGerestService(
                vertx.createHttpClient());
        this.externalCacheService = new DefaultWidgetExternalCacheService(config, vertx.createHttpClient(new HttpClientOptions()));
        this.config = config;
        super.init(vertx, config, rm, securedActions);
    }

    @Get("/:uai/cantine/menu")
    public void fetchMenu(final HttpServerRequest httpServerRequest) {
//      call service method fetch menu
        log.info("[WebGerest] - fetch menu");
        String uai = httpServerRequest.params().get("uai");

        if (uai == null || uai.isEmpty()) {
            // If uai is not provided, respond with a bad request error;
            httpServerRequest.response().setStatusCode(400).end("Missing UAI parameter");
            return;
        }

        webGerestService.getMenu(uai, config, defaultResponseHandler(httpServerRequest, 200));
    }
}
