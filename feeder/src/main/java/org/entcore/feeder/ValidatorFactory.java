/*
 * Copyright © WebServices pour l'Éducation, 2017
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

package org.entcore.feeder;

import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.webutils.DefaultAsyncResult;
import org.entcore.feeder.csv.CsvValidator;
import org.entcore.feeder.exceptions.ValidationException;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

public class ValidatorFactory {

	private final Vertx vertx;

	public ValidatorFactory(Vertx vertx) {
		this.vertx = vertx;
	}

	public void validator(String importId, final Handler<AsyncResult<ImportValidator>> handler) {
		MongoDb.getInstance().findOne("imports", new JsonObject().put("_id", importId), new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				final JsonObject result = event.body().getJsonObject("result");
				if ("ok".equals(event.body().getString("status")) && result != null) {
					switch (result.getString("source")) {
						case "CSV" :
							handler.handle(new DefaultAsyncResult<ImportValidator>(new CsvValidator(vertx, event.body().getString("language", "fr"), result)));
							break;
						default:
							handler.handle(new DefaultAsyncResult<ImportValidator>(
									new ValidationException("undefined.validator")));
					}
				} else {
					handler.handle(new DefaultAsyncResult<ImportValidator>(
							new ValidationException(event.body().getString("message"))));
				}
			}
		});
	}

}
