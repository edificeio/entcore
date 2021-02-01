package org.entcore.session;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

public interface ActivityManager {

    long LAST_ACTIVITY_DELAY = 3 * 60000l;

    void updateLastActivity(String sessionId, String userId, boolean secureLocation, Handler<AsyncResult<Void>> handler);

    void getLastActivity(String sessionId, boolean secureLocation, Handler<AsyncResult<Long>> handler);

    void removeLastActivity(String sessionId, Handler<AsyncResult<Void>> handler);

    boolean isEnabled();

}
