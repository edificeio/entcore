package org.entcore.common.events;

import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.Utils;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.user.UserInfos;

public class EventHelper {
    private static final Logger logger = LoggerFactory.getLogger(EventHelper.class);
    public static final String ACCESS_EVENT = "ACCESS";
    public static final String CREATE_EVENT = "CREATE";
    private final EventStore store;

    public EventHelper(final EventStore aStore) {
        this.store = aStore;
    }

    public void onAccess(final HttpServerRequest request) {
        // #WB2-1957: Skip creating ACCESS EVENT for mobile webview
        final String xApp = request.getParam("xApp");
        if ("mobile".equals(xApp)) {
            return;
        }

        try {
            store.createAndStoreEvent(ACCESS_EVENT, request);
        } catch (Exception e) {
            logger.error("ACCESS event failed", e);
        }
    }

    public void onCreateResource(final UserInfos user, final String resourceType, final MultiMap headers) {
        try {
            final String ua = headers.get("User-Agent");
            final JsonObject attrs = new JsonObject().put("resource-type", resourceType);
            if (ua != null) {
                attrs.put("ua", ua);
            }
            final String ip = headers.get("X-Forwarded-For");
            if (Utils.isNotEmpty(ip)) {
                attrs.put("ip", ip);
            }
            store.createAndStoreEvent(CREATE_EVENT, user, attrs);
        } catch (Exception e) {
            logger.error("CREATE event failed", e);
        }
    }

    public void onCreateResource(final HttpServerRequest request, final String resourceType) {
        try {
            store.createAndStoreEvent(CREATE_EVENT, request, new JsonObject().put("resource-type", resourceType));
        } catch (Exception e) {
            logger.error("CREATE event failed", e);
        }
    }

    public <T> Handler<T> onCreateResource(final HttpServerRequest request, final String resourceType, final Handler<T> handler) {
        final Handler<T> h = (r) -> {
            handler.handle(r);
            if (r instanceof AsyncResult) {
                final AsyncResult<?> tmp = (AsyncResult<?>) r;
                if (tmp.succeeded()) {
                    this.onCreateResource(request, resourceType);
                }
            } else if (r instanceof Either) {
                final Either<?, ?> tmp = (Either<?, ?>) r;
                if (tmp.isRight()) {
                    this.onCreateResource(request, resourceType);
                }
            }
        };
        return h;
    }

    public EventStore getStore() {
        return store;
    }
}
