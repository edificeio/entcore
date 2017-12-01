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

package org.entcore.common.events.impl;

import fr.wseduc.webutils.Either;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

public class BusEventStore extends GenericEventStore {

	@Override
	protected void storeEvent(final JsonObject event, final Handler<Either<String, Void>> handler) {
		eventBus.send("event.store", event, new Handler<AsyncResult<Message<JsonObject>>>(){
			@Override
			public void handle(AsyncResult<Message<JsonObject>> res) {
				if (res.succeeded()) {
					handler.handle(new Either.Right<String, Void>(null));
				} else {
					handler.handle(new Either.Left<String, Void>(
							"Error : " + res.cause().getMessage() + ", Event : " + event.encode()));
				}
			}
		});
	}

}
