/* Copyright © "Open Digital Education", 2014
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
						JsonArray a = new fr.wseduc.webutils.collections.JsonArray();
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
			JsonArray r = res.body().getJsonArray("result", new fr.wseduc.webutils.collections.JsonArray());
			return new Either.Right<>(r);
		} else {
			return new Either.Left<>(res.body().getString("message", ""));
		}
	}

	public static Either<String, JsonArray> validResults(Message<JsonObject> res) {
		if ("ok".equals(res.body().getString("status"))) {
			return new Either.Right<>(res.body().getJsonArray("results", new fr.wseduc.webutils.collections.JsonArray()));
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
