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

	/**
	 * Stores an event after checking for duplicates.
	 * <p>
	 * For ACCESS events, this method checks whether the same user has already accessed
	 * the same module recently (via session attribute {@code lastAccessModule}). If a
	 * duplicate is detected the event is silently dropped and the handler is called with
	 * a successful result, avoiding double-counting in analytics.
	 * </p>
	 * <p>
	 * The default implementation performs no deduplication and delegates directly to
	 * {@link #store(JsonObject, Handler)}. Override to provide deduplication logic.
	 * </p>
	 *
	 * @param event     the event payload to store
	 * @param user      the authenticated user, used to read and update the session dedup state
	 * @param module    the application module name (passed explicitly to avoid unreliable JSON extraction)
	 * @param eventType the event type (e.g. {@code "ACCESS"}); non-ACCESS events are always stored
	 * @param handler   called with {@code Right<Void>} on success (including skipped duplicates),
	 *                  or {@code Left<String>} on storage error
	 */
	default void storeWithCheck(JsonObject event, UserInfos user, String module, String eventType, Handler<Either<String, Void>> handler) {
		store(event, handler);
	}

	void generateMobileEvent(String eventType, UserInfos user, HttpServerRequest request, String module, final Handler<Either<String, Void>> handler);

	void storeCustomEvent(String baseEventType, JsonObject payload);

  @Deprecated
	void listEvents(String eventStoreType, long startEpoch, long duration, boolean skipSynced, List<String> eventTypes, boolean sorted, Handler<AsyncResult<JsonArray>> handler);

  void listEventsPartial(String eventStoreType, long startEpoch, long duration, boolean skipSynced, List<String> eventTypes, boolean sorted, Handler<AsyncResult<PartialResults>> handler);

	void markSyncedEvents(String eventStoreType, long startEpoch, long duration, Handler<AsyncResult<JsonObject>> handler);

}
