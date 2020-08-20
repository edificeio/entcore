package org.entcore.infra.controllers;

import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import org.entcore.common.email.impl.PostgresEmailHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.SuperAdminFilter;

import java.util.Base64;
import java.util.UUID;

import static fr.wseduc.webutils.Utils.isNotEmpty;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;

public class MailController extends BaseController {
    private final PostgresEmailHelper helper;
    private final String image;
    public MailController(Vertx vertx, JsonObject config){
        this.image = config.getString("tracking-image", "R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7");
        this.helper = new PostgresEmailHelper(vertx, config.getJsonObject("postgresql"));
    }

    @Get("/mail/:id")
    public void checkScanReport(final HttpServerRequest request) {
        final String id = request.getParam("id");
        helper.setRead(true, UUID.fromString(id));
        final HttpServerResponse response = request.response();
        response.putHeader("Content-Type", "image/png");
        response.putHeader("Cache-Control", "no-cache, no-store, must-revalidate, max-age=0");
        response.end(Buffer.buffer(Base64.getDecoder().decode(image)));
    }
}
