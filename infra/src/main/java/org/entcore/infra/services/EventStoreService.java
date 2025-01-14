/*
 * Copyright © "Open Digital Education", 2014
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

 */

package org.entcore.infra.services;

import fr.wseduc.webutils.Either;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Arrays;
import java.util.List;

import org.entcore.common.user.UserInfos;


public interface EventStoreService {

	List<String> EVENT_STORE_TYPES = Arrays.asList("events", "traces");
	long ONE_DAY_DURATION = 24 * 3600 * 1000L;

	void store(JsonObject event, Handler<Either<String, Void>> handler);

	void generateMobileEvent(String eventType, UserInfos user, HttpServerRequest request, String module, final Handler<Either<String, Void>> handler);

	void storeCustomEvent(String baseEventType, JsonObject payload);

	void listEvents(String eventStoreType, long startEpoch, long duration, boolean skipSynced, List<String> eventTypes, Handler<AsyncResult<JsonArray>> handler);

	void markSyncedEvents(String eventStoreType, long startEpoch, long duration, Handler<AsyncResult<JsonObject>> handler);

}
