/*
 * Copyright © WebServices pour l'Éducation, 2015
 *
 * This file is part of ENT Core. ENT Core is a versatile ENT engine based on the JVM.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * For the sake of explanation, any module that communicate over native
 * Web protocols, such as HTTP, with ENT Core is outside the scope of this
 * license and could be license under its own terms. This is merely considered
 * normal use of ENT Core, and does not fall under the heading of "covered work".
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
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
					JsonArray ids = new JsonArray();
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
