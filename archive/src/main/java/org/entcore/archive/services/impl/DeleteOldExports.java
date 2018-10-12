/*
 * Copyright © "Open Digital Education", 2015
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

package org.entcore.archive.services.impl;


import fr.wseduc.mongodb.MongoDb;
import org.entcore.archive.Archive;
import org.entcore.common.storage.Storage;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Calendar;
import java.util.Date;

public class DeleteOldExports implements Handler<Long> {

	private final MongoDb mongo = MongoDb.getInstance();
	private final Storage storage;
	private final int delay;

	public DeleteOldExports(Storage storage, int delay) {
		this.storage = storage;
		this.delay = delay;
	}

	@Override
	public void handle(Long event) {
		Calendar c = Calendar.getInstance();
		c.setTime(new Date());
		c.add(Calendar.HOUR, - delay);
		final JsonObject query = new JsonObject()
				.put("date", new JsonObject()
						.put("$lt", new JsonObject()
								.put("$date", c.getTime().getTime())));
		mongo.find(Archive.ARCHIVES, query, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				JsonArray res = event.body().getJsonArray("results");
				if ("ok".equals(event.body().getString("status")) && res != null && res.size() > 0) {
					JsonArray ids = new fr.wseduc.webutils.collections.JsonArray();
					for (Object object: res) {
						if (!(object instanceof JsonObject)) continue;
						ids.add(((JsonObject) object).getString("file_id"));
					}
					storage.removeFiles(ids, new Handler<JsonObject>() {
						@Override
						public void handle(JsonObject event) {
							mongo.delete(Archive.ARCHIVES, query);
						}
					});
				}
			}
		});
	}

}
