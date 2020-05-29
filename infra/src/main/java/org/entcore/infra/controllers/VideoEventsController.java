package org.entcore.infra.controllers;

import fr.wseduc.rs.Post;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.entcore.common.events.video.VideoEventsLogger;

public class VideoEventsController extends BaseController {

    private static final VideoEventsLogger LOGGER = new VideoEventsLogger();

    @Post("/video/event")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void videoEvent(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject body) {
                LOGGER.info(body);
                renderJson(request, new JsonObject().put("ok", "video event logged").put("event", body), 201);
            }
        });
    }
}
