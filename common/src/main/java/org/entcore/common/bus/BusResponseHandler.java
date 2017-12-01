/*
 * Copyright © WebServices pour l'Éducation, 2014
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

package org.entcore.common.bus;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class BusResponseHandler {

	private BusResponseHandler() {}

	public static Handler<Either<String, JsonObject>> busResponseHandler(final Message<JsonObject> message) {
		return new Handler<Either<String, JsonObject>>() {
			@Override
			public void handle(Either<String, JsonObject> event) {
				if (event.isRight()) {
					message.reply(new JsonObject().put("status", "ok")
							.put("result", event.right().getValue()));
				} else {
					JsonObject error = new JsonObject()
							.put("status", "error")
							.put("message", event.left().getValue());
					message.reply(error);
				}
			}
		};
	}

	public static Handler<Either<String, JsonArray>> busArrayHandler(final Message<JsonObject> message) {
		return new Handler<Either<String, JsonArray>>() {
			@Override
			public void handle(Either<String, JsonArray> event) {
				if (event.isRight()) {
					message.reply(new JsonObject().put("status", "ok")
							.put("result", event.right().getValue()));
				} else {
					JsonObject error = new JsonObject()
							.put("status", "error")
							.put("message", event.left().getValue());
					message.reply(error);
				}
			}
		};
	}

}
