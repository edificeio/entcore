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

package org.entcore.common.sql;

import fr.wseduc.webutils.Either;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public class SqlResult {

	public static Long countResult(Message<JsonObject> res) {
		if ("ok".equals(res.body().getString("status"))) {
			JsonArray values = res.body().getArray("results");
			if (values != null && values.size() == 1) {
				JsonArray row = values.get(0);
				if (row != null && row.size() == 1) {
					return row.get(0);
				}
			}
		}
		return null;
	}

	public static Either<String, JsonObject> validUniqueResult(Message<JsonObject> res) {
		Either<String, JsonArray> r = validResult(res);
		if (r.isRight()) {
			JsonArray results = r.right().getValue();
			if (results == null || results.size() == 0) {
				return new Either.Right<>(new JsonObject());
			}
			if (results.size() == 1 && (results.get(0) instanceof JsonObject)) {
				return new Either.Right<>((JsonObject) results.get(0));
			}
			return new Either.Left<>("non.unique.result");
		} else {
			return new Either.Left<>(r.left().getValue());
		}
	}

	public static Either<String, JsonArray> validResult(Message<JsonObject> res) {
		if ("ok".equals(res.body().getString("status"))) {
			JsonArray f = res.body().getArray("fields");
			JsonArray r = res.body().getArray("results");
			if (f != null && r != null) {
				JsonArray result = new JsonArray();
				for (Object o : r) {
					if (!(o instanceof JsonArray)) continue;
					JsonArray a = (JsonArray) o;
					JsonObject j = new JsonObject();
					for (int i = 0; i < f.size(); i++) {
						Object item = a.get(i);
						if (item instanceof Boolean) {
							j.putBoolean((String) f.get(i), (Boolean) item);
						} else if (item instanceof Number) {
							j.putNumber((String) f.get(i), (Number) item);
						} else if (item != null) {
							j.putString((String) f.get(i), item.toString());
						} else {
							j.putValue((String) f.get(i), null);
						}
					}
					result.addObject(j);
				}
				return new Either.Right<>(result);
			} else {
				return new Either.Right<>(new JsonArray());
			}
		} else {
			return new Either.Left<>(res.body().getString("message", ""));
		}
	}

	public static Either<String, JsonObject> validRowsResult(Message<JsonObject> res) {
		if ("ok".equals(res.body().getString("status"))) {
			long rows = res.body().getLong("rows", 0l);
			JsonObject result = new JsonObject();
			if(rows > 0){
				result.putNumber("rows", rows);
			}
			return new Either.Right<>(result);
		} else {
			return new Either.Left<>(res.body().getString("message", ""));
		}
	}

	public static Handler<Message<JsonObject>> validRowsResultHandler(
			final Handler<Either<String, JsonObject>> handler) {
		return new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				handler.handle(validRowsResult(event));
			}
		};
	}

	public static Handler<Message<JsonObject>> validUniqueResultHandler(
			final Handler<Either<String, JsonObject>> handler) {
		return new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				handler.handle(validUniqueResult(event));
			}
		};
	}

	public static Handler<Message<JsonObject>> validResultHandler(
			final Handler<Either<String, JsonArray>> handler) {
		return new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				handler.handle(validResult(event));
			}
		};
	}

}
