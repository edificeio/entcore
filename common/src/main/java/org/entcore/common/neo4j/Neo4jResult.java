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

package org.entcore.common.neo4j;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class Neo4jResult {

	public static Either<String, JsonObject> fullNodeMerge(String nodeAttr,
			Message<JsonObject> res, String... otherNodes) {
		Either<String, JsonObject> r = validUniqueResult(res);
		if (r.isRight() && r.right().getValue().size() > 0) {
			JsonObject j = r.right().getValue();
			JsonObject data = j.getJsonObject(nodeAttr, new JsonObject()).getJsonObject("data");
			if (otherNodes != null && otherNodes.length > 0) {
				for (String attr : otherNodes) {
					Object e = j.getValue(attr);
					if (e == null) continue;
					if (e instanceof JsonObject) {
						data.put(attr, ((JsonObject) e).getJsonObject("data"));
					} else if (e instanceof JsonArray) {
						JsonArray a = new JsonArray();
						for (Object o : (JsonArray) e) {
							if (!(o instanceof JsonObject)) continue;
							JsonObject jo = (JsonObject) o;
							a.add(jo.getJsonObject("data"));
						}
						data.put(attr, a);
					}
					j.remove(attr);
				}
			}
			if (data != null) {
				j.remove(nodeAttr);
				return new Either.Right<>(data.mergeIn(j));
			}
		}
		return r;
	}

	public static Either<String, JsonObject> validUniqueResult(Message<JsonObject> res) {
		Either<String, JsonArray> r = validResult(res);
		if (r.isRight()) {
			JsonArray results = r.right().getValue();
			if (results == null || results.size() == 0) {
				return new Either.Right<>(new JsonObject());
			}
			if (results.size() == 1 && (results.getValue(0) instanceof JsonObject)) {
				return new Either.Right<>(results.getJsonObject(0));
			}
			return new Either.Left<>("non.unique.result");
		} else {
			return new Either.Left<>(r.left().getValue());
		}
	}

	public static Either<String, JsonObject> validEmpty(Message<JsonObject> res) {
		if ("ok".equals(res.body().getString("status"))) {
			return new Either.Right<>(new JsonObject());
		} else {
			return new Either.Left<>(res.body().getString("message", ""));
		}
	}

	public static Either<String, JsonArray> validResult(Message<JsonObject> res) {
		if ("ok".equals(res.body().getString("status"))) {
			JsonArray r = res.body().getJsonArray("result", new JsonArray());
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

	private static Either<String, JsonObject> validUniqueResult(int idx, Message<JsonObject> event) {
		Either<String, JsonArray> r = validResults(event);
		if (r.isRight()) {
			JsonArray results = r.right().getValue();
			if (results == null || results.size() == 0) {
				return new Either.Right<>(new JsonObject());
			} else {
				results = results.getJsonArray(idx);
				if (results.size() == 1 && (results.getValue(0) instanceof JsonObject)) {
					return new Either.Right<>(results.getJsonObject(0));
				}
			}
			return new Either.Left<>("non.unique.result");
		} else {
			return new Either.Left<>(r.left().getValue());
		}
	}

	public static Handler<Message<JsonObject>> fullNodeMergeHandler(final String nodeAttr,
			final Handler<Either<String, JsonObject>> handler, final String... otherNodes) {
		return new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				handler.handle(fullNodeMerge(nodeAttr, event, otherNodes));
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

	public static Handler<Message<JsonObject>> validUniqueResultHandler(final int idx,
			final Handler<Either<String, JsonObject>> handler) {
		return new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				handler.handle(validUniqueResult(idx, event));
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

	public static Handler<Message<JsonObject>> validEmptyHandler(
			final Handler<Either<String, JsonObject>> handler) {
		return new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				handler.handle(validEmpty(event));
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
