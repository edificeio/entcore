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

import java.util.HashMap;
import java.util.Map;

import fr.wseduc.mongodb.MongoDb;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.core.impl.VertxInternal;

import static fr.wseduc.webutils.Utils.getOrElse;

public abstract class AbstractSessionStore implements SessionStore {

    protected static final Logger logger = LoggerFactory.getLogger(AbstractSessionStore.class);
    protected final long sessionTimeout;
    protected final long prolongedSessionTimeout;
    protected final Vertx vertx;
    protected Map<String, Long> inactivity;

    public AbstractSessionStore(Vertx vertx, JsonObject config, Boolean cluster) {
        this.vertx = vertx;

        if (Boolean.TRUE.equals(cluster)) {
            final ClusterManager cm = ((VertxInternal) vertx).getClusterManager();
            if (getOrElse(config.getBoolean("inactivity"), false)) {
                inactivity = cm.getSyncMap("inactivity");
                logger.info("inactivity ha map : " + inactivity.getClass().getName());
            }
        } else {
            if (getOrElse(config.getBoolean("inactivity"), false)) {
                inactivity = new HashMap<>();
            }
        }

        Object timeout = config.getValue("session_timeout");
		if (timeout != null) {
			if (timeout instanceof Long) {
				this.sessionTimeout = (Long)timeout;
			} else if (timeout instanceof Integer) {
				this.sessionTimeout = (Integer)timeout;
			} else {
                this.sessionTimeout = DEFAULT_SESSION_TIMEOUT;
            }
		} else {
			this.sessionTimeout = DEFAULT_SESSION_TIMEOUT;
		}
		Object prolongedTimeout = config.getValue("prolonged_session_timeout");
		if (prolongedTimeout != null) {
			if (prolongedTimeout instanceof Long) {
				this.prolongedSessionTimeout = (Long)prolongedTimeout;
			} else if (prolongedTimeout instanceof Integer) {
				this.prolongedSessionTimeout = (Integer)prolongedTimeout;
			} else {
                this.prolongedSessionTimeout = 20 * DEFAULT_SESSION_TIMEOUT;
            }
		} else {
			this.prolongedSessionTimeout = 20 * DEFAULT_SESSION_TIMEOUT;
		}
    }

    protected long setTimer(final String userId, final String sessionId, final boolean secureLocation) {
        if (inactivity != null) {
            inactivity.put(sessionId, System.currentTimeMillis());
        }
        return setTimer(userId, sessionId, sessionTimeout, secureLocation);
    }

    protected long setTimer(final String userId, final String sessionId, final long sessionTimeout,
            final boolean secureLocation) {
        return vertx.setTimer(sessionTimeout, timerId -> {
            if (inactivity != null) {
                final Long lastActivity = inactivity.get(sessionId);
                if (lastActivity != null) {
                    final long timeoutTimestamp = lastActivity
                            + (secureLocation ? prolongedSessionTimeout : sessionTimeout);
                    final long now = System.currentTimeMillis();
                    if (timeoutTimestamp > now) {
                        setTimer(userId, sessionId, (timeoutTimestamp - now), secureLocation);
                    } else {
                        dropSession(sessionId, null);
                    }
                } else {
                    dropSession(sessionId, null);
                }
            } else {
                removeCacheSession(userId, sessionId);
            }
        });
    }

    protected abstract void removeCacheSession(String userId, String sessionId);

    protected void dropMongoDbSession(String sessionId) {
        MongoDb.getInstance().delete(AuthManager.SESSIONS_COLLECTION, new JsonObject().put("_id", sessionId));
    }

}
