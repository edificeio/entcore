package org.entcore.timeline.util;

import fr.wseduc.mongodb.MongoDb;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * MongoDB helpers for raw commands sent through the persistor.
 */
public final class MongoUtil {

	private MongoUtil() {
	}

	/**
	 * Runs an {@code aggregate} command and drains its cursor with {@code getMore} until it is exhausted.
	 * A single command reply is capped at 16 MB whatever the batchSize, so reading only {@code firstBatch}
	 * silently truncates large results. Fails if the aggregate or any getMore fails, never returns a partial result.
	 *
	 * @param aggregation full aggregate command ({@code aggregate}, {@code pipeline}, {@code cursor}...)
	 * @param options delivery options applied to every round trip, may be null
	 */
	public static Future<JsonArray> aggregateAll(MongoDb mongo, JsonObject aggregation, DeliveryOptions options) {
		final String collection = aggregation.getString("aggregate");
		final Promise<JsonArray> promise = Promise.promise();
		mongo.command(aggregation.encode(), options, reply -> {
			final JsonObject body = reply.body();
			if (!MongoDb.isOk(body)) {
				promise.fail("aggregate on " + collection + " failed: " + MongoDb.toErrorStr(body));
				return;
			}
			final JsonObject cursor = body.getJsonObject("result", new JsonObject()).getJsonObject("cursor", new JsonObject());
			final JsonArray results = cursor.getJsonArray("firstBatch", new JsonArray());
			drain(mongo, collection, cursor.getLong("id", 0L), options, results, promise);
		});
		return promise.future();
	}

	private static void drain(MongoDb mongo, String collection, long cursorId, DeliveryOptions options,
							  JsonArray results, Promise<JsonArray> promise) {
		if (cursorId == 0L) {
			promise.complete(results);
			return;
		}
		final JsonObject getMore = new JsonObject().put("getMore", cursorId).put("collection", collection);
		mongo.command(getMore.encode(), options, reply -> {
			final JsonObject body = reply.body();
			if (!MongoDb.isOk(body)) {
				promise.fail("getMore on " + collection + " failed after " + results.size() + " documents: " + MongoDb.toErrorStr(body));
				return;
			}
			final JsonObject cursor = body.getJsonObject("result", new JsonObject()).getJsonObject("cursor", new JsonObject());
			results.addAll(cursor.getJsonArray("nextBatch", new JsonArray()));
			drain(mongo, collection, cursor.getLong("id", 0L), options, results, promise);
		});
	}
}
