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

import static org.entcore.common.neo4j.Neo4jResult.validUniqueResultHandler;

import java.util.ArrayList;
import java.util.List;

import org.entcore.common.http.request.JsonHttpServerRequest;
import org.entcore.common.notification.TimelineHelper;
import org.entcore.common.service.impl.BasicQuotaService;
import org.entcore.workspace.service.WorkspaceService;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class DefaultQuotaService extends BasicQuotaService {

	private final TimelineHelper notification;
	private final boolean neo4jPlugin;

	public DefaultQuotaService(boolean neo4jPlugin, TimelineHelper timelineHelper) {
		this.neo4jPlugin = neo4jPlugin;
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
		if (!neo4jPlugin) {
			String query = "MATCH (u:UserBook { userid : {userId}}) " + "SET u.__lock__ = 1, u.storage = u.storage + {size} "
					+ "WITH u, u.alertSize as oldAlert "
					+ "SET u.alertSize = ((100.0 * u.storage / u.quota) > {threshold}), u.__lock__ = 0 "
					+ "RETURN u.storage as storage, (u.alertSize = true AND oldAlert <> u.alertSize) as notify ";
			params.put("userId", userId);
			neo4j.execute(query, params, validUniqueResultHandler(handler));
		} else {
			neo4j.unmanagedExtension("put", "/entcore/quota/storage/" + userId, params.encode(),
					new Handler<Message<JsonObject>>() {
						@Override
						public void handle(Message<JsonObject> event) {
							if ("ok".equals(event.body().getString("status"))) {
								handler.handle(new Either.Right<String, JsonObject>(
										new JsonObject(event.body().getString("result"))));
							} else {
								handler.handle(new Either.Left<String, JsonObject>(event.body().getString("message")));
							}
						}
					});
		}
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
