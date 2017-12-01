/* Copyright © WebServices pour l'Éducation, 2014
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
 *
 */

package org.entcore.common.mongodb;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class MongoDbResult {


	public static Either<String, JsonObject> validActionResult(Message<JsonObject> res) {
		if ("ok".equals(res.body().getString("status"))) {
			res.body().remove("status");
			return new Either.Right<>(res.body());
		} else {
			return new Either.Left<>(res.body().getString("message", ""));
		}
	}

	public static Either<String, JsonObject> validResult(Message<JsonObject> res) {
		if ("ok".equals(res.body().getString("status"))) {
			JsonObject r = res.body().getJsonObject("result", new JsonObject());
			return new Either.Right<>(r);
		} else {
			return new Either.Left<>(res.body().getString("message", ""));
		}
	}

	public static Either<String, JsonArray> validResults(Message<JsonObject> res) {
		if ("ok".equals(res.body().getString("status"))) {
			return new Either.Right<>(res.body().getJsonArray("results", new JsonArray()));
		} else {
			return new Either.Left<>(res.body().getString("message", ""));
		}
	}

	public static Handler<Message<JsonObject>> validActionResultHandler(
			final Handler<Either<String, JsonObject>> handler) {
		return new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				handler.handle(validActionResult(event));
			}
		};
	}

	public static Handler<Message<JsonObject>> validResultHandler(
			final Handler<Either<String, JsonObject>> handler) {
		return new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				handler.handle(validResult(event));
			}
		};
	}

	public static Handler<Message<JsonObject>> validResultsHandler(
			final Handler<Either<String, JsonArray>> handler) {
		return new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				handler.handle(validResults(event));
			}
		};
	}

}
