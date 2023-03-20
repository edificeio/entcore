/* Copyright © "Open Digital Education", 2019
 *
 * This program is published by "Open Digital Education".
 * You must indicate the name of the software and the company in any production /contribution
 * using the software and indicate on the home page of the software industry in question,
 * "powered by Open Digital Education" with a reference to the website: https://opendigitaleducation.com/.
 *
 * This program is free software, licensed under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation, version 3 of the License.
 *
 * You can redistribute this application and/or modify it since you respect the terms of the GNU Affero General Public License.
 * If you modify the source code and then use this modified source code in your creation, you must make available the source code of your modifications.
 *
 * You should have received a copy of the GNU Affero General Public License along with the software.
 * If not, please see : <http://www.gnu.org/licenses/>. Full compliance requires reading the terms of this license and following its directives.
 *
 */

package org.entcore.session;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.spi.cluster.ClusterManager;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapSessionStore extends AbstractSessionStore {

    protected Map<String, String> sessions;
    protected Map<String, List<LoginInfo>> logins;

    private static final class LoginInfo implements Serializable {
        long timerId;
        final String sessionId;

        private LoginInfo(long timerId, String sessionId) {
            this.timerId = timerId;
            this.sessionId = sessionId;
        }
    }

    public MapSessionStore(final Vertx vertx, final Boolean cluster, JsonObject config) {
        super(vertx, config, cluster);
        inactivity = new MapActivityManager(vertx, config, cluster);
        if (Boolean.TRUE.equals(cluster)) {
            final ClusterManager cm = ((VertxInternal) vertx).getClusterManager();
            sessions = cm.getSyncMap("sessions");
            logins = cm.getSyncMap("logins");
            logger.info("Initialize session cluster maps.");
        } else {
            sessions = new HashMap<>();
            logins = new HashMap<>();
            logger.info("Initialize session hash maps.");
        }
    }

    @Override
    public void getSession(final String sessionId, final Handler<AsyncResult<JsonObject>> handler) {
        JsonObject session = null;
        try {
            session = unmarshal(sessions.get(sessionId));
        } catch (Exception e) {
            logger.warn("Error in deserializing hazelcast session " + sessionId);
            try {
                sessions.remove(sessionId);
            } catch (Exception e1) {
                logger.warn("Error getting object after removing hazelcast session " + sessionId);
            }
        }
        if (session != null) {
            final String userId = session.getString("userId");
            final boolean secureLocation = (session.getJsonObject("sessionMetadata") != null ? 
                    session.getJsonObject("sessionMetadata").getBoolean("secureLocation", false) : false);
            if (inactivityEnabled() && userId != null) {
                inactivity.updateLastActivity(sessionId, userId, secureLocation, ar -> {
                    if (ar.failed()) {
                        logger.error("Error when update last activity with session " + sessionId, ar.cause());
                    }
                });
            }
            handler.handle(Future.succeededFuture(session));
        } else {
            handler.handle(Future.failedFuture(new SessionException("Session not found")));
        }
    }

    @Override
    public void listSessionsIds(String userId, Handler<AsyncResult<JsonArray>> handler) {
        final List<LoginInfo> loginInfos = logins.get(userId);
        if (loginInfos != null) {
            final JsonArray sessionIds = new JsonArray();
            for (LoginInfo loginInfo : loginInfos) {
                sessionIds.add(loginInfo.sessionId);
            }
            handler.handle(Future.succeededFuture(sessionIds));
        } else {
            handler.handle(Future.failedFuture(new SessionException("Login not found")));
        }
    }


    @Override
    public void getSessionByUserId(String userId, Handler<AsyncResult<JsonObject>> handler) {
        LoginInfo info = getLoginInfo(userId);
        if (info == null) {
            handler.handle(Future.failedFuture(new SessionException("User not found in session")));
            return;
        }
        JsonObject session = null;
        try {
            session = unmarshal(sessions.get(info.sessionId));
        } catch (Exception e) {
            logger.error("Error in deserializing hazelcast session " + info.sessionId, e);
        }
        if (session == null) {
            handler.handle(Future.failedFuture(new SessionException("Session not found")));
        } else {
            handler.handle(Future.succeededFuture(session));
        }
    }

    private LoginInfo getLoginInfo(String userId) {
        List<LoginInfo> loginInfos = logins.get(userId);
        if (loginInfos != null && !loginInfos.isEmpty()) {
            return loginInfos.get(loginInfos.size() - 1);
        }
        return null;
    }

    private LoginInfo getLoginInfo(String sessionId, String userId) {
        List<LoginInfo> loginInfos = logins.get(userId);
        LoginInfo loginInfo = null;
        if (loginInfos != null && sessionId != null) {
            for (LoginInfo i : loginInfos) {
                if (sessionId.equals(i.sessionId)) {
                    return loginInfo;
                }
            }
        }
        return null;
    }

    private JsonObject unmarshal(String s) {
        if (s != null) {
            return new JsonObject(s);
        }
        return null;
    }

    private void addLoginInfo(String userId, long timerId, String sessionId) {
        List<LoginInfo> loginInfos = logins.get(userId);
        if (loginInfos == null) {
            loginInfos = new ArrayList<>();
        }
        loginInfos.add(new LoginInfo(timerId, sessionId));
        logins.put(userId, loginInfos);
    }

    @Override
    public void putSession(String userId, String sessionId, JsonObject infos, boolean secureLocation,
            Handler<AsyncResult<Void>> handler) {
        long timerId = setTimer(userId, sessionId, secureLocation);

        try {
            sessions.put(sessionId, infos.encode());
            addLoginInfo(userId, timerId, sessionId);
            handler.handle(Future.succeededFuture());
        } catch (Exception e) {
            logger.error("Error putting session in hazelcast map");
            handler.handle(Future.failedFuture(new SessionException("Error putting session in hazelcast map")));
        }
    }

    private LoginInfo removeLoginInfo(String sessionId, String userId) {
        List<LoginInfo> loginInfos = logins.get(userId);
        LoginInfo loginInfo = null;
        if (loginInfos != null && sessionId != null) {
            boolean found = false;
            int idx = 0;
            for (LoginInfo i : loginInfos) {
                if (sessionId.equals(i.sessionId)) {
                    found = true;
                    break;
                }
                idx++;
            }
            if (found) {
                loginInfo = loginInfos.remove(idx);
                if (loginInfos.isEmpty()) {
                    logins.remove(userId);
                } else {
                    logins.put(userId, loginInfos);
                }
            }
        }
        return loginInfo;
    }

    @Override
    public void dropSession(String sessionId, Handler<AsyncResult<JsonObject>> handler) {
        JsonObject session = null;
        try {
            session = unmarshal(sessions.get(sessionId));
        } catch (Exception e) {
            try {
                sessions.remove(sessionId);
            } catch (Exception e1) {
                logger.error("In doDrop - Error getting object after removing hazelcast session " + sessionId, e);
            }
        }
        if (session != null) {
            JsonObject s = unmarshal(sessions.remove(sessionId));
            if (s != null) {
                final String userId = s.getString("userId");
                LoginInfo info = removeLoginInfo(sessionId, userId);
                if (info != null) {
                    vertx.cancelTimer(info.timerId);
                }
                if (handler != null) {
                    handler.handle(Future.succeededFuture(s));
                }
            } else {
                if (handler != null) {
                    handler.handle(Future.succeededFuture(session));
                }
            }
        } else {
            if (handler != null) {
                handler.handle(Future.failedFuture(new SessionException("Session not found when drop")));
            }
        }
        if (inactivityEnabled()) {
            inactivity.removeLastActivity(sessionId, ar -> {
                if (ar.failed()) {
                    logger.error("Error when update last activity with session " + sessionId, ar.cause());
                }
            });
            dropMongoDbSession(sessionId);
        }

    }

    private JsonObject getSessionByUserId(String userId) {
        LoginInfo info = getLoginInfo(userId);
        if (info == null) { // disconnected user : ignore action
            return null;
        }
        JsonObject session = null;
        try {
            session = unmarshal(sessions.get(info.sessionId));
        } catch (Exception e) {
            logger.error("Error in deserializing hazelcast session " + info.sessionId, e);
        }
        if (session == null) {
            return null;
        }
        return session;
    }

    private JsonObject getSessionBySessionId(String sessionId) {
        JsonObject session = null;
        try {
            session = unmarshal(sessions.get(sessionId));
        } catch (Exception e) {
            logger.error("Error in deserializing hazelcast session " + sessionId, e);
        }
        if (session == null) {
            return null;
        }
        return session;
    }

    private void updateCacheAttributeByUserId(String userId, String key, Object value) throws SessionException {
        List<LoginInfo> infos = logins.get(userId);
        if (infos == null || infos.isEmpty()) {
            throw new SessionException("LoginInfo not found");
        }
        for (LoginInfo info : infos) {
            try {
                JsonObject session = unmarshal(sessions.get(info.sessionId));
                if (session != null) {
                    session.getJsonObject("cache").put(key, value);
                    sessions.put(info.sessionId, session.encode());
                } else {
                    logger.error("Error getting session in hazelcast map : " + info.sessionId);
                }
            } catch (Exception e) {
                logger.error("Error putting session in hazelcast map : " + info.sessionId, e);
            }
        }
    }

    private void removeCacheAttributeByUserId(String userId, String key) throws SessionException {
        List<LoginInfo> infos = logins.get(userId);
        if (infos == null || infos.isEmpty()) {
            throw new SessionException("LoginInfo not found");
        }
        for (LoginInfo info : infos) {
            try {
                JsonObject session = unmarshal(sessions.get(info.sessionId));
                if (session != null) {
                    session.getJsonObject("cache").remove(key);
                    sessions.put(info.sessionId, session.encode());
                } else {
                    logger.error("Error getting session in hazelcast map : " + info.sessionId);
                }
            } catch (Exception e) {
                logger.error("Error putting session in hazelcast map : " + info.sessionId, e);
            }
        }
    }

    @Override
    public void addCacheAttribute(String sessionId, String key, Object value, Handler<AsyncResult<Void>> handler) {
        final JsonObject session = getSessionBySessionId(sessionId);
        if (session == null) {
            handler.handle(Future.failedFuture(new SessionException("Session not found when add attribute : " + sessionId)));
            return;
        }

        session.getJsonObject("cache").put(key, value);
        try {
            sessions.put(sessionId, session.encode());
            handler.handle(Future.succeededFuture());
        } catch (Exception e) {
            logger.error("Error putting session in hazelcast map : " + sessionId, e);
            handler.handle(Future.failedFuture(new SessionException("Error putting session in hazelcast map: " + sessionId)));
        }
    }

    @Override
    public void dropCacheAttribute(String sessionId, String key, Handler<AsyncResult<Void>> handler) {
        // TODO Auto-generated method stub

    }

    @Override
    public void addCacheAttributeByUserId(String userId, String key, Object value, Handler<AsyncResult<Void>> handler) {
        JsonObject session = getSessionByUserId(userId);
        if (session == null) {
            handler.handle(Future.failedFuture(new SessionException("Session not found when add attribute : " + userId)));
            return;
        }

        try {
            updateCacheAttributeByUserId(userId, key, value);
            handler.handle(Future.succeededFuture());
        } catch (SessionException e) {
            handler.handle(Future.failedFuture(new SessionException("Session not found when update add attribute: " + userId)));
        }
    }

    @Override
    public void dropCacheAttributeByUserId(String userId, String key, Handler<AsyncResult<Void>> handler) {
        JsonObject session = getSessionByUserId(userId);
        if (session == null) {
            handler.handle(Future.failedFuture(new SessionException("Session not found when drop attribute : " + userId)));
            return;
        }

		try {
            removeCacheAttributeByUserId(userId, key);
            handler.handle(Future.succeededFuture());
        } catch (SessionException e) {
            handler.handle(Future.failedFuture(new SessionException("Session not found when update drop attribute: " + userId)));
        }
    }

    @Override
    protected void removeCacheSession(String userId, String sessionId) {
        logins.remove(userId);
        sessions.remove(sessionId);
    }

    @Override
    public void getSessionsNumber(Handler<AsyncResult<Long>> handler) {
        handler.handle(Future.succeededFuture(0L));
    }

    @Override
    protected void updateTimerId(String userId, String sessionId, long timerId) {
        final LoginInfo loginInfo = getLoginInfo(sessionId, userId);
        if (loginInfo != null) {
            loginInfo.timerId = timerId;
        }
    }

}
