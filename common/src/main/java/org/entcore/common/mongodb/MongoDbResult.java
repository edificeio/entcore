/*
 * Copyright. Tous droits réservés. WebServices pour l’Education.
 */

package org.entcore.common.mongodb;

import fr.wseduc.webutils.Either;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public class MongoDbResult {


	public static Either<String, JsonObject> validActionResult(Message<JsonObject> res) {
		if ("ok".equals(res.body().getString("status"))) {
			res.body().removeField("status");
			return new Either.Right<>(res.body());
		} else {
			return new Either.Left<>(res.body().getString("message", ""));
		}
	}

	public static Either<String, JsonObject> validResult(Message<JsonObject> res) {
		if ("ok".equals(res.body().getString("status"))) {
			JsonObject r = res.body().getObject("result", new JsonObject());
			return new Either.Right<>(r);
		} else {
			return new Either.Left<>(res.body().getString("message", ""));
		}
	}

	public static Either<String, JsonArray> validResults(Message<JsonObject> res) {
		if ("ok".equals(res.body().getString("status"))) {
			return new Either.Right<>(res.body().getArray("results", new JsonArray()));
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
