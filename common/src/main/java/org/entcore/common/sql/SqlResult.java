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
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.*;

public class SqlResult {

	public static Long countResult(Message<JsonObject> res) {
		if ("ok".equals(res.body().getString("status"))) {
			JsonArray values = res.body().getJsonArray("results");
			if (values != null && values.size() == 1) {
				JsonArray row = values.getJsonArray(0);
				if (row != null && row.size() == 1) {
					return row.getLong(0);
				}
			}
		}
		return null;
	}

	public static Either<String, JsonObject> validUniqueResult(Message<JsonObject> res) {
		Either<String, JsonArray> r = validResult(res);
		return validUnique(r);
	}

	private static Either<String, JsonObject> validUnique(Either<String, JsonArray> r) {
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

	public static Either<String, JsonObject> validUniqueResult(int idx, Message<JsonObject> res) {
		Either<String, JsonArray> r = validResult(idx, res);
		return validUnique(r);
	}

	public static Either<String, JsonArray> validResult(int idx, Message<JsonObject> res) {
		if ("ok".equals(res.body().getString("status"))) {
			JsonArray a = res.body().getJsonArray("results");
			if (a != null && idx < a.size()) {
				return jsonToEither(a.getJsonObject(idx)
					.put("jsonb_fields", res.body().getJsonArray("jsonb_fields", new JsonArray())));
			} else {
				return new Either.Left<>("missing.result");
			}
		} else {
			return new Either.Left<>(res.body().getString("message", ""));
		}
	}

	public static Either<String, JsonArray> validResults(Message<JsonObject> res) {
		if ("ok".equals(res.body().getString("status"))) {
			JsonArray a = res.body().getJsonArray("results");
			JsonArray r = new JsonArray();
			for (Object o : a) {
				if (!(o instanceof JsonObject)) continue;
				r.add(transform((JsonObject) o));
			}
			return new Either.Right<>(r);
		} else {
			return new Either.Left<>(res.body().getString("message", ""));
		}
	}

	public static Either<String, JsonArray> validResult(Message<JsonObject> res) {
		return jsonToEither(res.body());
	}

	private static Either<String, JsonArray> jsonToEither(JsonObject body) {
		if ("ok".equals(body.getString("status"))) {
			return new Either.Right<>(transform(body));
		} else {
			return new Either.Left<>(body.getString("message", ""));
		}
	}

	private static JsonArray transform(JsonObject body) {
		JsonArray f = body.getJsonArray("fields");
		JsonArray r = body.getJsonArray("results");
		JsonArray result = new JsonArray();
		if (f != null && r != null) {
			JsonArray jsonbAttributes = body.getJsonArray("jsonb_fields");
			List ja = (jsonbAttributes != null) ? jsonbAttributes.getList() : new ArrayList<>();
			for (Object o : r) {
				if (!(o instanceof JsonArray)) continue;
				JsonArray a = (JsonArray) o;
				JsonObject j = new JsonObject();
				for (int i = 0; i < f.size(); i++) {
					Object item = a.getValue(i);
					if (item instanceof Boolean) {
						j.put(f.getString(i), (Boolean) item);
					} else if (item instanceof Number) {
						j.put(f.getString(i), (Number) item);
					} else if (item instanceof JsonArray) {
						j.put(f.getString(i), (JsonArray) item);
					} else if (item != null && ja.contains(f.getValue(i))) {
						String stringRepresentation = item.toString().trim();
						if(stringRepresentation.startsWith("[")){
							j.put(f.getString(i), new JsonArray(item.toString()));
						} else {
							j.put(f.getString(i), new JsonObject(item.toString()));
						}
					} else if (item != null) {
						j.put(f.getString(i), item.toString());
					} else {
						j.put(f.getString(i), (String) null);
					}
				}
				result.add(j);
			}
		}
		return result;
	}

	public static Either<String, JsonObject> validRowsResult(Message<JsonObject> res) {
		JsonObject body = res.body();
		return validRows(body);
	}

	private static Either<String, JsonObject> validRows(JsonObject body) {
		if ("ok".equals(body.getString("status"))) {
			long rows = body.getLong("rows", 0l);
			JsonObject result = new JsonObject();
			if(rows > 0){
				result.put("rows", rows);
			}
			return new Either.Right<>(result);
		} else {
			return new Either.Left<>(body.getString("message", ""));
		}
	}

	public static Either<String, JsonObject> validRowsResult(int idx, Message<JsonObject> res) {
		if ("ok".equals(res.body().getString("status"))) {
			JsonArray a = res.body().getJsonArray("results");
			if (a != null && idx < a.size()) {
				return validRows(a.getJsonObject(idx));
			} else {
				return new Either.Left<>("missing.result");
			}
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

	public static Handler<Message<JsonObject>> validRowsResultHandler(final int idx,
			final Handler<Either<String, JsonObject>> handler) {
		return new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				handler.handle(validRowsResult(idx, event));
			}
		};
	}

	public static Handler<Message<JsonObject>> validUniqueResultHandler(
			final Handler<Either<String, JsonObject>> handler, final String... jsonbFields) {
		return new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				if (jsonbFields != null && jsonbFields.length > 0) {
					event.body().put("jsonb_fields", new JsonArray(Arrays.asList(jsonbFields)));
				}
				handler.handle(validUniqueResult(event));
			}
		};
	}

	public static Handler<Message<JsonObject>> validUniqueResultHandler(final int idx,
			final Handler<Either<String, JsonObject>> handler, final String... jsonbFields) {
		return new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				if (jsonbFields != null && jsonbFields.length > 0) {
					event.body().put("jsonb_fields", new JsonArray(Arrays.asList(jsonbFields)));
				}
				handler.handle(validUniqueResult(idx, event));
			}
		};
	}

	public static Handler<Message<JsonObject>> validResultHandler(final int idx,
			final Handler<Either<String, JsonArray>> handler, final String... jsonbFields) {
		return new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				if (jsonbFields != null && jsonbFields.length > 0) {
					event.body().put("jsonb_fields", new JsonArray(Arrays.asList(jsonbFields)));
				}
				handler.handle(validResult(idx, event));
			}
		};
	}

	public static Handler<Message<JsonObject>> validResultHandler(
			final Handler<Either<String, JsonArray>> handler, final String... jsonbFields) {
		return new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				if (jsonbFields != null && jsonbFields.length > 0) {
					event.body().put("jsonb_fields", new JsonArray(Arrays.asList(jsonbFields)));
				}
				handler.handle(validResult(event));
			}
		};
	}

	public static Handler<Message<JsonObject>> validResultsHandler(
			final Handler<Either<String, JsonArray>> handler, final String... jsonbFields) {
		return new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				if (jsonbFields != null && jsonbFields.length > 0) {
					event.body().put("jsonb_fields", new JsonArray(Arrays.asList(jsonbFields)));
				}
				handler.handle(validResults(event));
			}
		};
	}

	public static Handler<Message<JsonObject>> parseSharedUnique(final Handler<Either<String, JsonObject>> handler) {
		return new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> message) {
				Either<String, JsonObject> res = validUniqueResult(message);
				if (res.isRight()) {
					JsonObject j = res.right().getValue();
					parseShared(j);
					handler.handle(res);
				} else {
					handler.handle(res);
				}
			}
		};
	}

	private static void parseShared(JsonObject j) {
		Map<String, JsonObject> shared = new HashMap<>();
		JsonArray a = new JsonArray();
		JsonArray s = new JsonArray(j.getString("shared"));
		JsonArray m = new JsonArray(j.getString("groups"));
		for (Object o : s) {
			if (o == null || !(o instanceof JsonObject)) continue;
			JsonObject json = (JsonObject) o;
			String member = json.getString("member_id");
			String action = json.getString("action");
			if (member != null && action != null) {
				if (shared.containsKey(member)) {
					shared.get(member).put(action, true);
				} else {
					JsonObject sj = new JsonObject().put(action, true);
					if (m.contains(member)) {
						sj.put("groupId", member);
					} else {
						sj.put("userId", member);
					}
					shared.put(member, sj);
					a.add(sj);
				}
			}
		}
		j.remove("groups");
		j.put("shared", a);
	}

	public static Handler<Message<JsonObject>> parseShared(final Handler<Either<String, JsonArray>> handler) {
		return new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> message) {
				Either<String, JsonArray> res = validResult(message);
				if (res.isRight()) {
					JsonArray out = new JsonArray();
					for (Object row : res.right().getValue()) {
						if (!(row instanceof JsonObject)) continue;
						JsonObject j = (JsonObject) row;
						parseShared(j);
						out.add(j);
					}
					handler.handle(new Either.Right<String, JsonArray>(out));
				} else {
					handler.handle(res);
				}
			}
		};

	}

}
