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

package org.entcore.workspace.service.impl;

import static org.entcore.common.neo4j.Neo4jResult.validResultHandler;
import static org.entcore.common.neo4j.Neo4jResult.validUniqueResultHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.entcore.common.http.request.JsonHttpServerRequest;
import org.entcore.common.notification.TimelineHelper;
import org.entcore.common.service.impl.BasicQuotaService;
import org.entcore.workspace.service.WorkspaceService;

import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class DefaultQuotaService extends BasicQuotaService {

	private static final Logger log = LoggerFactory.getLogger(DefaultQuotaService.class);
	/** Max users per Neo4j transaction : every user of a batch stays locked until its commit. */
	static final int STORAGE_BATCH_SIZE = 500;
	/** Same update as incrementStorage, for a list of {userId, size}. */
	private static final String INCREMENT_STORAGES_QUERY =
			"UNWIND {users} AS row " +
			"MATCH (u:UserBook { userid : row.userId }) " +
			"SET u.__lock__ = 1, u.storage = u.storage + row.size " +
			"WITH u, u.alertSize as oldAlert " +
			"SET u.alertSize = ((100.0 * u.storage / u.quota) > {threshold}), u.__lock__ = 0 " +
			"RETURN u.userid as userId, u.storage as storage, (u.alertSize = true AND oldAlert <> u.alertSize) as notify ";

	private final TimelineHelper notification;

	public DefaultQuotaService(TimelineHelper timelineHelper) {
		this.notification = timelineHelper;
	}

	@Override
	public void notifySmallAmountOfFreeSpace(String userId) {
		List<String> recipients = new ArrayList<>();
		recipients.add(userId);
		notification.notifyTimeline(new JsonHttpServerRequest(new JsonObject()),
				WorkspaceService.WORKSPACE_NAME.toLowerCase() + ".storage", null, recipients, null, new JsonObject());
	}

	@Override
	public void incrementStorage(String userId, Long size, int threshold,
			final Handler<Either<String, JsonObject>> handler) {
		JsonObject params = new JsonObject().put("size", size).put("threshold", threshold);
		String query = "MATCH (u:UserBook { userid : {userId}}) " + "SET u.__lock__ = 1, u.storage = u.storage + {size} "
				+ "WITH u, u.alertSize as oldAlert "
				+ "SET u.alertSize = ((100.0 * u.storage / u.quota) > {threshold}), u.__lock__ = 0 "
				+ "RETURN u.storage as storage, (u.alertSize = true AND oldAlert <> u.alertSize) as notify ";
		params.put("userId", userId);
		neo4j.execute(query, params, validUniqueResultHandler(handler));
	}

	/**
	 * {@link #incrementStorage} for several users at once. Sizes are summed per user and users sorted by id, so that
	 * concurrent calls lock them in the same order and cannot deadlock, then updated by transactions of
	 * {@link #STORAGE_BATCH_SIZE} users. A failed transaction is logged and skipped : the previous ones are committed,
	 * their rows must still reach the caller.
	 * @param users array of {userId, size}
	 * @return rows of {userId, storage, notify}, only for the users found and updated
	 */
	public Future<JsonArray> incrementStorages(JsonArray users, int threshold) {
		final Map<String, Long> sizes = new TreeMap<>();
		for (Object o : users) {
			final JsonObject user = (JsonObject) o;
			if (user.getString("userId") != null) {
				sizes.merge(user.getString("userId"), user.getLong("size", 0L), Long::sum);
			}
		}
		final List<JsonArray> batches = new ArrayList<>();
		JsonArray batch = new JsonArray();
		for (Map.Entry<String, Long> size : sizes.entrySet()) {
			if (batch.size() == STORAGE_BATCH_SIZE) {
				batches.add(batch);
				batch = new JsonArray();
			}
			batch.add(new JsonObject().put("userId", size.getKey()).put("size", size.getValue()));
		}
		if (!batch.isEmpty()) {
			batches.add(batch);
		}
		Future<JsonArray> updated = Future.succeededFuture(new JsonArray());
		for (JsonArray b : batches) {
			updated = updated.compose(rows -> incrementStoragesBatch(b, threshold).map(rows::addAll));
		}
		return updated;
	}

	private Future<JsonArray> incrementStoragesBatch(JsonArray users, int threshold) {
		final Promise<JsonArray> promise = Promise.promise();
		final JsonObject params = new JsonObject().put("users", users).put("threshold", threshold);
		neo4j.execute(INCREMENT_STORAGES_QUERY, params, validResultHandler(result -> {
			if (result.isLeft()) {
				log.error("Failed updating the storage of " + users.size() + " users : " + result.left().getValue());
				promise.complete(new JsonArray());
			} else {
				promise.complete(result.right().getValue());
			}
		}));
		return promise.future();
	}

	@Override
	public void decrementStorage(String userId, Long size, int threshold, Handler<Either<String, JsonObject>> handler) {
		incrementStorage(userId, -1l * size, threshold, handler);
	}

	@Override
	public void quotaAndUsage(String userId, Handler<Either<String, JsonObject>> handler) {
		super.quotaAndUsage(userId, handler);
	}

	@Override
	public void quotaAndUsageStructure(String structureId, Handler<Either<String, JsonObject>> handler) {
		super.quotaAndUsageStructure(structureId, handler);
	}

	@Override
	public void quotaAndUsageGlobal(Handler<Either<String, JsonObject>> handler) {
		super.quotaAndUsageGlobal(handler);
	}

	@Override
	public void update(JsonArray users, long quota, Handler<Either<String, JsonArray>> handler) {
		super.update(users, quota, handler);
	}

	@Override
	public void updateQuotaDefaultMax(String profile, Long defaultQuota, Long maxQuota,
			Handler<Either<String, JsonObject>> handler) {
		super.updateQuotaDefaultMax(profile, defaultQuota, maxQuota, handler);
	}

	@Override
	public void getDefaultMaxQuota(Handler<Either<String, JsonArray>> handler) {
		super.getDefaultMaxQuota(handler);
	}

	@Override
	public void init(final String userId) {
		super.init(userId);
	}

}
