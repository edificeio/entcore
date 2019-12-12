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
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface SessionStore {

    long DEFAULT_SESSION_TIMEOUT = 30 * 60 * 1000;

    long LAST_ACTIVITY_DELAY = 60000l;

    void getSession(String sessionId, Handler<AsyncResult<JsonObject>> handler);

    void listSessionsIds(String userId, Handler<AsyncResult<JsonArray>> handler);

    void getSessionByUserId(String userId, Handler<AsyncResult<JsonObject>> handler);

    void putSession(String userId, String sessionId, JsonObject infos, boolean secureLocation, Handler<AsyncResult<Void>> handler);

    void dropSession(String sessionId, Handler<AsyncResult<JsonObject>> handler);

    void addCacheAttribute(String sessionId, String key, Object value, Handler<AsyncResult<Void>> handler);

    void dropCacheAttribute(String sessionId, String key, Handler<AsyncResult<Void>> handler);

    void addCacheAttributeByUserId(String userId, String key, Object value, Handler<AsyncResult<Void>> handler);

    void dropCacheAttributeByUserId(String userId, String key, Handler<AsyncResult<Void>> handler);

}
