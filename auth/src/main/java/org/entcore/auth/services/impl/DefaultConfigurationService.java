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

package org.entcore.auth.services.impl;

import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.Utils;
import org.entcore.auth.services.ConfigurationService;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

import static fr.wseduc.webutils.Utils.isNotEmpty;
import static org.entcore.common.mongodb.MongoDbResult.validActionResultHandler;

public class DefaultConfigurationService implements ConfigurationService {

	private final MongoDb mongoDb = MongoDb.getInstance();
	private static final String WELCOME_MESSAGE_TYPE = "WELCOME_MESSAGE";
	public static final String PLATEFORM_COLLECTION = "platform";

	@Override
	public void editWelcomeMessage(String domain, JsonObject messages, Handler<Either<String, JsonObject>> handler) {
		if (Utils.defaultValidationParamsNull(handler, domain, messages)) return;
		final JsonObject q = new JsonObject().put("type", WELCOME_MESSAGE_TYPE);
		final JsonObject modifier = new JsonObject().put("$set", new JsonObject().put(domain.replaceAll("\\.", "_"), messages));
		mongoDb.update(PLATEFORM_COLLECTION, q, modifier, true, false, validActionResultHandler(handler));
	}

	@Override
	public void getWelcomeMessage(String domain, String language, final Handler<Either<String, JsonObject>> handler) {
		final JsonObject q = new JsonObject().put("type", WELCOME_MESSAGE_TYPE);
		JsonObject keys = null;
		if (isNotEmpty(domain) && isNotEmpty(language)) {
			keys = new JsonObject();
			keys.put("_id", 0);
			keys.put(domain.replaceAll("\\.", "_") + "." + language, 1);
			keys.put(domain.replaceAll("\\.", "_") + ".enabled", 1);
		} else if (isNotEmpty(domain)) {
			keys = new JsonObject();
			keys.put("_id", 0);
			keys.put(domain.replaceAll("\\.", "_"), 1);
		}
		mongoDb.findOne(PLATEFORM_COLLECTION, q, keys, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> res) {
				if ("ok".equals(res.body().getString("status"))) {
					JsonObject r = res.body().getJsonObject("result", new JsonObject());
					JsonObject j = new JsonObject();
					for (String attr : r.fieldNames()) {
						j.put(attr.replaceAll("_", "."), r.getValue(attr));
					}
					handler.handle(new Either.Right<String, JsonObject>(j));
				} else {
					handler.handle(new Either.Left<String, JsonObject>(res.body().getString("message", "")));
				}
			}
		});
	}

}
