/*
 * Copyright © "Open Digital Education", 2014
 *
 * This program is published by "Open Digital Education".
 * You must indicate the name of the software and the company in any production /contribution
 * using the software and indicate on the home page of the software industry in question,
 * "powered by Open Digital Education" with a reference to the website: https://opendigitaleducation.com/.
 *
 * This program is free software, licensed under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation, version 3 of the License.
 *
 * You can redistribute this application and/or modify it since you respect the terms of the GNU Affero General Public License.
 * If you modify the source code and then use this modified source code in your creation, you must make available the source code of your modifications.
 *
 * You should have received a copy of the GNU Affero General Public License along with the software.
 * If not, please see : <http://www.gnu.org/licenses/>. Full compliance requires reading the terms of this license and following its directives.

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
					.put("jsonb_fields", res.body().getJsonArray("jsonb_fields", new fr.wseduc.webutils.collections.JsonArray())));
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
			JsonArray r = new fr.wseduc.webutils.collections.JsonArray();
			for (Object o : a) {
				if (!(o instanceof JsonObject)) continue;
				r.add(transform((JsonObject) o));
			}
			return new Either.Right<>(r);
		} else {
			return new Either.Left<>(res.body().getString("message", ""));
		}
	}

	public static Either<String, JsonArray> validGroupedResults(Message<JsonObject> res) {
		if ("ok".equals(res.body().getString("status"))) {
			JsonArray results = res.body().getJsonArray("results");
			JsonArray fields = res.body().getJsonArray("fields");
			JsonArray transformedResults = new JsonArray();
			for (Object raw : results) {
				if (!(raw instanceof JsonArray)) continue;
				final JsonArray result = (JsonArray) raw;
				final JsonObject transformed = new JsonObject();
				for(int i = 0; i < fields.size(); i++) {
					final String fieldName = fields.getString(i);
					transformed.put(fieldName, result.getValue(i));
				}
				transformedResults.add(transformed);
			}
			return new Either.Right<>(transformedResults);
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
		JsonArray result = new fr.wseduc.webutils.collections.JsonArray();
		if (f != null && r != null) {
			JsonArray jsonbAttributes = body.getJsonArray("jsonb_fields");
			List ja = (jsonbAttributes != null) ? jsonbAttributes.getList() : new ArrayList<>();
			for (Object o : r) {
				if (!(o instanceof JsonArray)) continue;
				JsonArray a = (JsonArray) o;
				JsonObject j = new fr.wseduc.webutils.collections.JsonObject();
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
							j.put(f.getString(i), new fr.wseduc.webutils.collections.JsonArray(item.toString()));
						} else {
							j.put(f.getString(i), new fr.wseduc.webutils.collections.JsonObject(item.toString()));
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
					event.body().put("jsonb_fields", new fr.wseduc.webutils.collections.JsonArray(Arrays.asList(jsonbFields)));
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
					event.body().put("jsonb_fields", new fr.wseduc.webutils.collections.JsonArray(Arrays.asList(jsonbFields)));
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
					event.body().put("jsonb_fields", new fr.wseduc.webutils.collections.JsonArray(Arrays.asList(jsonbFields)));
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
					event.body().put("jsonb_fields", new fr.wseduc.webutils.collections.JsonArray(Arrays.asList(jsonbFields)));
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
					event.body().put("jsonb_fields", new fr.wseduc.webutils.collections.JsonArray(Arrays.asList(jsonbFields)));
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

	public static void parseShared(JsonObject j) {
		Map<String, JsonObject> shared = new HashMap<>();
		JsonArray a = new fr.wseduc.webutils.collections.JsonArray();
		JsonArray s = new fr.wseduc.webutils.collections.JsonArray(j.getString("shared"));
		JsonArray m = new fr.wseduc.webutils.collections.JsonArray(j.getString("groups"));
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

	public static void parseSharedFromArray(JsonObject j) {
		Map<String, JsonObject> shared = new HashMap<>();
		JsonArray a = new fr.wseduc.webutils.collections.JsonArray();
		JsonArray s = j.getJsonArray("shared");
		JsonArray m = j.getJsonArray("groups");
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
					JsonArray out = new fr.wseduc.webutils.collections.JsonArray();
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

	public static <T> List<T> sqlArrayToList(JsonArray array, Class<T> clazz) {
		final List<T> list = new ArrayList<>();
		if (array != null) {
			for (Object o: array) {
				if (!(o instanceof JsonArray)) continue;
				final JsonArray item = (JsonArray) o;
				if (item.size() == 2) {
					final Object itemValue = item.getValue(1);
					if (clazz.isInstance(itemValue)) {
						list.add((T) itemValue);
					}
				}
			}
		}
		return list;
	}

}
