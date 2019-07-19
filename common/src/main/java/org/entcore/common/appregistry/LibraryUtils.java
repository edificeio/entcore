package org.entcore.common.appregistry;

import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

import static fr.wseduc.webutils.http.Renders.renderJson;

public final class LibraryUtils {
    public static final String LIBRARY_BUS_ADDRESS = "wse.app.registry.library.add";

    public static void share(EventBus eb, final HttpServerRequest request) {
        eb.send(LIBRARY_BUS_ADDRESS, new JsonObject(), r -> {
            if(r.succeeded()) {
                JsonObject message = (JsonObject) r.result().body();
                boolean success = message.getBoolean("success", false);
                if (success) {
                    renderJson(request, message, 200);
                } else {
                    renderJson(request, message, 500);
                }
            } else {
                JsonObject message = new JsonObject()
                        .put("error", r.cause().getMessage());
                renderJson(request, message, 500);
            }
        });
    }
}
