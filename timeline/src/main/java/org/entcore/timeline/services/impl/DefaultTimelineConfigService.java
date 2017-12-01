/*
 * Copyright © WebServices pour l'Éducation, 2016
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

package org.entcore.timeline.services.impl;

import static org.entcore.common.mongodb.MongoDbResult.*;

import org.entcore.common.service.impl.MongoDbCrudService;
import org.entcore.timeline.services.TimelineConfigService;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import fr.wseduc.webutils.Either;

public class DefaultTimelineConfigService extends MongoDbCrudService implements TimelineConfigService {

	public DefaultTimelineConfigService(String collection) {
		super(collection);
	}

	@Override
	public void upsert(JsonObject data, Handler<Either<String, JsonObject>> handler) {
		final String key = data.getString("key");
		if(key == null){
			handler.handle(new Either.Left<String, JsonObject>("invalid.key"));
			return;
		}
		mongo.update(collection, new JsonObject().put("key", key), data, true, false, validActionResultHandler(handler));
	}

	@Override
	public void list(Handler<Either<String, JsonArray>> handler) {
		JsonObject sort = new JsonObject().put("modified", -1);
		mongo.find(collection, new JsonObject("{}"), sort, defaultListProjection, validResultsHandler(handler));
	}

}
