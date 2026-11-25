package org.entcore.conversation.util;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.user.UserUtils;

import java.util.Map;

/**
 * Storage counter updates, held by the workspace quota service.
 */
public final class QuotaUtil {

	private static final Logger log = LoggerFactory.getLogger(QuotaUtil.class);
	/** Bus address of the workspace quota service. */
	public static final String QUOTA_BUS_ADDRESS = "org.entcore.workspace.quota";
	/** Storage alert threshold in percent, when the configuration has no alertStorage. */
	public static final int DEFAULT_STORAGE_ALERT_THRESHOLD = 80;
	private static final String STORAGE_FIELD = "storage";

	private QuotaUtil() {
	}

	/**
	 * Adds a size to the storage of several users in one call, then refreshes the storage kept in their session, which
	 * bounds their uploads. Sizes of zero are skipped. A failure is logged and answered with no rows : the storage
	 * counter is only ever updated by differences, the caller has nothing left to undo.
	 * @param eb the event bus
	 * @param sizes size to add per user id, negative to give storage back
	 * @param threshold storage alert threshold, in percent
	 * @return rows of {userId, storage, notify}, for the users updated
	 */
	public static Future<JsonArray> updateUsersQuota(final EventBus eb, final Map<String, Long> sizes, final int threshold) {
		final JsonArray users = new JsonArray();
		sizes.forEach((userId, size) -> {
			if (size != null && size != 0) {
				users.add(new JsonObject().put("userId", userId).put("size", size));
			}
		});
		if (users.isEmpty()) {
			return Future.succeededFuture(new JsonArray());
		}
		final JsonObject message = new JsonObject()
				.put("action", "updateUsersQuota")
				.put("users", users)
				.put("threshold", threshold);
		final Promise<JsonArray> promise = Promise.promise();
		eb.<JsonObject>request(QUOTA_BUS_ADDRESS, message, reply -> {
			if (reply.failed() || !"ok".equals(reply.result().body().getString("status"))) {
				log.error("Failed updating the storage of " + users.size() + " users : "
						+ (reply.failed() ? reply.cause().getMessage() : reply.result().body().getString("message")));
				promise.complete(new JsonArray());
				return;
			}
			final JsonArray rows = reply.result().body().getJsonArray("results", new JsonArray());
			for (Object o : rows) {
				final JsonObject row = (JsonObject) o;
				if (row.getLong(STORAGE_FIELD) != null) {
					UserUtils.addSessionAttribute(eb, row.getString("userId"), STORAGE_FIELD, row.getLong(STORAGE_FIELD), null);
				}
			}
			promise.complete(rows);
		});
		return promise.future();
	}
}
