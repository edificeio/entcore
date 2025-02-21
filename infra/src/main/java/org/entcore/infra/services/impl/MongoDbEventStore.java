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

package org.entcore.infra.services.impl;

import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.CookieHelper;
import io.vertx.core.http.HttpServerRequest;

import static org.entcore.common.mongodb.MongoDbResult.validAsyncActionResultHandler;
import static org.entcore.common.mongodb.MongoDbResult.validAsyncResultsHandler;

import java.util.ArrayList;
import java.util.List;

import org.entcore.common.events.impl.PostgresqlEventStore;
import org.entcore.common.user.UserInfos;
import org.entcore.infra.services.EventStoreService;

import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class MongoDbEventStore implements EventStoreService {

	private static final long QUERY_TIMEOUT = 90000L;
	private MongoDb mongoDb = MongoDb.getInstance();
	private PostgresqlEventStore pgEventStore;
	private static final String COLLECTION = "events";

	public MongoDbEventStore(Vertx vertx) {
		final String eventStoreConf = (String) vertx.sharedData().getLocalMap("server").get("event-store");
		if (eventStoreConf != null) {
			final JsonObject eventStoreConfig = new JsonObject(eventStoreConf);
			if (eventStoreConfig.containsKey("postgresql")) {
				pgEventStore =  new PostgresqlEventStore();
				pgEventStore.setEventBus(vertx.eventBus());
				pgEventStore.setModule("infra");
				pgEventStore.setVertx(vertx);
				pgEventStore.init();
			}
		}
	}

	@Override
	public void store(JsonObject event, final Handler<Either<String, Void>> handler) {
		mongoDb.save(COLLECTION, event, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				if ("ok".equals(event.body().getString("status"))) {
					handler.handle(new Either.Right<String, Void>(null));
				} else {
					handler.handle(new Either.Left<String, Void>(
							"Error : " + event.body().getString("message") + ", Event : " + event));
				}
			}
		});
		if (pgEventStore != null) {
			pgEventStore.store(event.copy(), ar -> {
			});
		}
	}

	@Override
	public void generateMobileEvent(String eventType, UserInfos user, HttpServerRequest request,
									 String module, final Handler<Either<String, Void>> handler) {
		JsonObject event = new JsonObject();
		event.put("event-type", eventType)
				.put("module", module)
				.put("date", System.currentTimeMillis());
		if (user != null) {
			event.put("userId", user.getUserId());
			if (user.getType() != null) {
				event.put("profil", user.getType());
			}
			if (user.getStructures() != null) {
				event.put("structures", new JsonArray(user.getStructures()));
			}
			if (user.getClasses() != null) {
				event.put("classes", new JsonArray(user.getClasses()));
			}
			if (user.getGroupsIds() != null) {
				event.put("groups", new JsonArray(user.getGroupsIds()));
			}
			if (request.headers().get("User-Agent") != null) {
				event.put("ua", request.headers().get("User-Agent"));
			}
		}
		if (request != null) {
			event.put("referer", request.headers().get("Referer"));
			event.put("sessionId", CookieHelper.getInstance().getSigned("oneSessionId", request));

			final String ip = Renders.getIp(request);
			if (ip != null) {
				event.put("ip", ip);
			}
		}
		store(event, handler);
	}

	@Override
	public void storeCustomEvent(String baseEventType, JsonObject payload) {
		if (pgEventStore != null) {
			pgEventStore.storeCustomEvent(baseEventType, payload);
		}
	}

	@Override
	public void listEvents(String eventStoreType, long startEpoch, long duration, boolean skipSynced, Handler<AsyncResult<JsonArray>> handler) {
		final JsonObject query = new JsonObject().put("date", new JsonObject()
			.put("$gte", startEpoch).put("$lt", (startEpoch + duration)));
		if (skipSynced) {
			query.put("synced", new JsonObject().put("$exists", false));
		}
		mongoDb.find(eventStoreType, query,  (JsonObject) null, (JsonObject) null, -1, -1, Integer.MAX_VALUE,
				new DeliveryOptions().setSendTimeout(QUERY_TIMEOUT), validAsyncResultsHandler(handler));
	}

	@Override
	public void markSyncedEvents(String eventStoreType, long startEpoch, long duration, Handler<AsyncResult<JsonObject>> handler) {
		final long endEpoch = (startEpoch + duration);
		final JsonObject query = new JsonObject()
			.put("date", new JsonObject()
				.put("$gte", startEpoch).put("$lt", endEpoch))
			.put("synced", new JsonObject().put("$exists", false));

		final JsonObject modifier = new JsonObject()
			.put("$set", new JsonObject().put("synced", new JsonObject().put("$date", endEpoch)));

		if ("traces".equals(eventStoreType)) {
			mongoDb.distinct(eventStoreType, "retention-days", query, validAsyncActionResultHandler(ar -> {
				if (ar.succeeded()) {
					final JsonArray values = ar.result().getJsonArray("values");
					if (values != null && values.size() > 0) {
						final List<Future> futures = new ArrayList<>();
						for (Object retention: values) {
							final JsonObject q = query.copy().put("retention-days", ((int) retention));
							final JsonObject m = new JsonObject().put("$set", new JsonObject()
								.put("synced", new JsonObject().put("$date", endEpoch + (((int) retention) * 24 * 3600 * 1000L)) ));
							futures.add(execMarkSyncedEvents(eventStoreType, q, m));
						}
						CompositeFuture.all(futures).onComplete(res -> {
							if (res.succeeded()) {
								handler.handle(Future.succeededFuture(new JsonObject()));
							} else {
								handler.handle(Future.failedFuture(res.cause()));
							}
						});
					} else {
						handler.handle(Future.succeededFuture(new JsonObject()));
					}
				} else {
					handler.handle(Future.failedFuture(ar.cause()));
				}
			}));
		} else {
			execMarkSyncedEvents(eventStoreType, query, modifier).onComplete(handler);
		}
	}

	private Future<JsonObject> execMarkSyncedEvents(String eventStoreType, JsonObject query, JsonObject modifier) {
		final Promise<JsonObject> promise = Promise.promise();
		mongoDb.update(eventStoreType, query, modifier, false, true, validAsyncActionResultHandler(ar -> {
			if (ar.succeeded()) {
				promise.complete(ar.result());
			} else {
				promise.fail(ar.cause());
			}
		}));
		return promise.future();
	}

}
