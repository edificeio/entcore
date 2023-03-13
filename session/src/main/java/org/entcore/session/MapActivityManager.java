package org.entcore.session;

import static fr.wseduc.webutils.Utils.getOrElse;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.spi.cluster.ClusterManager;

import java.util.HashMap;
import java.util.Map;

public class MapActivityManager implements ActivityManager {

    protected static final Logger logger = LoggerFactory.getLogger(MapActivityManager.class);
    private Map<String, Long> activity = null;

    public MapActivityManager(Vertx vertx, JsonObject config, Boolean cluster) {
        if (Boolean.TRUE.equals(cluster)) {
            final ClusterManager cm = ((VertxInternal) vertx).getClusterManager();
            if (getOrElse(config.getBoolean("inactivity"), false)) {
                activity = cm.getSyncMap("inactivity");
                logger.info("inactivity ha map : " + activity.getClass().getName());
            }
        } else {
            if (getOrElse(config.getBoolean("inactivity"), false)) {
                activity = new HashMap<>();
            }
        }
    }

    @Override
    public void updateLastActivity(String sessionId, String userId, final SessionMetadata sessionMetadata, Handler<AsyncResult<Void>> handler) {
        final long now = System.currentTimeMillis();
        final Long lastActivity = activity.get(sessionId);
        if (lastActivity == null || (lastActivity + LAST_ACTIVITY_DELAY) < now) {
            activity.put(sessionId, now);
        }
        handler.handle(Future.succeededFuture());
    }

    @Override
    public void getLastActivity(String sessionId, final SessionMetadata sessionMetadata, Handler<AsyncResult<Long>> handler) {
        handler.handle(Future.succeededFuture(activity.get(sessionId)));
    }

    @Override
    public void removeLastActivity(String sessionId, Handler<AsyncResult<Void>> handler) {
        activity.remove(sessionId);
        handler.handle(Future.succeededFuture());
    }

    @Override
    public boolean isEnabled() {
        return activity != null;
    }

}
