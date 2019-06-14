/*
 * Copyright © "Open Digital Education", 2019
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
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package org.entcore.directory.services.impl;

import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.webutils.Either;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.remote.RemoteClient;
import org.entcore.common.remote.RemoteClientResponse;
import org.entcore.directory.services.RemoteUserService;

import java.util.ArrayList;
import java.util.List;

import static fr.wseduc.webutils.Utils.isNotEmpty;

public class DefaultRemoteUserService implements RemoteUserService {

	private static final Logger log = LoggerFactory.getLogger(DefaultRemoteUserService.class);
	private MongoDb mongo = MongoDb.getInstance();
	private RemoteClient remoteClient;


	@Override
	public void oldPlatformsSync(String level, String excludeLevel, String profile, Handler<Either<String, JsonObject>> handler) {
		List<Future> futures = new ArrayList<>();
		String uri = "/directory/user/level/list?level=" + level + "&profile=" + profile;
		if (isNotEmpty(excludeLevel)) {
			uri += "&notLevel=" + excludeLevel;
		}
		remoteClient.getRemote(uri, futures);
		CompositeFuture.all(futures).map(CompositeFuture::list).setHandler(ar -> {
			if (ar.succeeded()) {
				final List<Future> futuresMongo = new ArrayList<>();
				JsonArray a = new JsonArray();
				final long importTimestamp = System.currentTimeMillis();
				for (Object o : ar.result()) {
					if (!(o instanceof RemoteClientResponse)) continue;
					final JsonArray r = new JsonArray(((RemoteClientResponse) o).getBody());
					for (Object o1 : r) {
						if (!(o1 instanceof JsonObject)) continue;
						final JsonObject j1 = (JsonObject) o1;
						final String id = (String) j1.remove("id");
						j1.put("_id", id).put("modified", importTimestamp);
						final JsonObject m = new JsonObject().put("$set", j1)
								.put("$setOnInsert", new JsonObject().put("created", importTimestamp));
						a.add(new JsonObject()
								.put("operation", "upsert")
								.put("document", m)
								.put("criteria", new JsonObject().put("_id", id)));
						if (a.size() % 1000 == 0) {
							futuresMongo.add(importOldPlateformsUsers(a));
							a = new JsonArray();
						}
					}
				}
				if (a.size() > 0) {
					futuresMongo.add(importOldPlateformsUsers(a));
				}
				CompositeFuture.all(futuresMongo).setHandler(ar2 -> {
					if (ar2.succeeded()) {
						handler.handle(new Either.Right<>(new JsonObject()));
					} else {
						handler.handle(new Either.Left<>(ar2.cause().getMessage()));
					}
				});
			} else {
				log.error("Error sync user by level on old plateforms.", ar.cause());
				handler.handle(new Either.Left<>(ar.cause().getMessage()));
			}
		});
	}

	private Future<Void> importOldPlateformsUsers(JsonArray buffer) {
		Future<Void> future = Future.future();
		mongo.bulk("oldplatformusers", buffer, event -> {
			if ("ok".equals(event.body().getString("status"))) {
				future.complete();
			} else {
				future.fail(new RuntimeException(event.body().getString("message")));
			}
		});
		return future;
	}

	public void setRemoteClient(RemoteClient remoteClient) {
		this.remoteClient = remoteClient;
	}

}
