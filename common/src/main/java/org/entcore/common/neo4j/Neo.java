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

import java.util.Map;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;

@Deprecated
public class Neo  {

	private Neo4j neo4j;

	public Neo (Vertx vertx, EventBus eb, Logger log) {
		neo4j = Neo4j.getInstance();
	}

	@Deprecated
	public void sendBatch(JsonArray queries, final Handler<Message<JsonObject>> handler) {
		neo4j.executeBatch(queries, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				if (handler != null) {
					JsonArray results = event.body().getJsonArray("results");
					if ("ok".equals(event.body().getString("status")) && results != null) {
						for (Object o : results) {
							if (!(o instanceof JsonObject)) continue;
							JsonObject j = (JsonObject) o;
							int i = 0;
							JsonObject r = new JsonObject();
							for (Object o2 : j.getJsonArray("result")) {
								if (!(o2 instanceof JsonObject)) continue;
								r.put(String.valueOf(i++), (JsonObject) o2);
							}
							j.put("result", r);
						}
					}
					handler.handle(event);
				}
			}
		});
	}

	@Deprecated
	public void sendBatch(JsonArray queries, final HttpServerResponse response) {
		sendBatch(queries, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> m) {
				response.putHeader("content-type", "text/json");
				response.end(m.body().encode());
			}
		});
	}

	@Deprecated
	public void send(String query, Handler<Message<JsonObject>> handler) {
		send(query, null, handler);
	}

	@Deprecated
	public void send(String query) {
		send(query, (Map<String,Object>) null);
	}

	@Deprecated
	public void send(String query, Map<String,Object> params, final HttpServerResponse response) {
		send(query, params, new Handler<Message<JsonObject>>() {
			public void handle(Message<JsonObject> m) {
				response.putHeader("content-type", "text/json");
				response.end(m.body().encode());
			}
		});
	}

	@Deprecated
	public void send(String query, Map<String,Object> params) {
		send(query, params, (Handler<Message<JsonObject>>) null);
	}

	@Deprecated
	public void send(String query, Map<String,Object> params, final Handler<Message<JsonObject>> handler) {
		if (handler != null) {
			neo4j.execute(query, params, new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> event) {
					JsonArray result = event.body().getJsonArray("result");
					if ("ok".equals(event.body().getString("status")) && result != null) {
						int i = 0;
						JsonObject r = new JsonObject();
						for (Object o : result) {
							if (!(o instanceof JsonObject)) continue;
							r.put(String.valueOf(i++), (JsonObject) o);
						}
						event.body().put("result", r);
					}
					handler.handle(event);
				}
			});
		} else {
			neo4j.execute(query, params, (Handler<Message<JsonObject>>) null);
		}
	}

	@Deprecated
	public void send(String query, final HttpServerResponse response) {
		send(query, null, response);
	}

	@Deprecated
	public static JsonObject toJsonObject(String query, JsonObject params) {
		return new JsonObject()
		.put("query", query)
		.put("params", (params != null) ? params : new JsonObject());
	}

	@Deprecated
	public static JsonArray resultToJsonArray(JsonObject j) {
		JsonArray r = new fr.wseduc.webutils.collections.JsonArray();
		if (j != null) {
			for (String idx : j.fieldNames()) {
				r.add(j.getJsonObject(idx));
			}
		}
		return r;
	}

	@Deprecated
	public void execute(String query, JsonObject params, Handler<Message<JsonObject>> handler) {
		neo4j.execute(query, params, handler);
	}

	@Deprecated
	public void execute(String query, Map<String,Object> params, Handler<Message<JsonObject>> handler) {
		execute(query, new JsonObject(params), handler);
	}

	@Deprecated
	public void execute(String query, Map<String,Object> params, final HttpServerResponse response) {
		execute(query, params, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> m) {
				response.putHeader("Content-Type", "application/json");
				response.end(m.body().encode());
			}
		});
	}

	@Deprecated
	public void execute(String query, JsonObject params, final HttpServerResponse response) {
		execute(query, params, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> m) {
				response.putHeader("Content-Type", "application/json");
				response.end(m.body().encode());
			}
		});
	}

	@Deprecated
	public void executeBatch(JsonArray queries, final Handler<Message<JsonObject>> handler) {
		neo4j.executeBatch(queries, handler);
	}

	@Deprecated
	public void executeBatch(JsonArray queries, final HttpServerResponse response) {
		executeBatch(queries, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> m) {
				response.putHeader("Content-Type", "application/json");
				response.end(m.body().encode());
			}
		});
	}

	@Deprecated
	public void executeTransaction(JsonArray statements, Integer transactionId, boolean commit,
			Handler<Message<JsonObject>> handler) {
		neo4j.executeTransaction(statements, transactionId, commit, handler);
	}

	@Deprecated
	public void resetTransactionTimeout(int transactionId, Handler<Message<JsonObject>> handler) {
		neo4j.resetTransactionTimeout(transactionId, handler);
	}

	@Deprecated
	public void rollbackTransaction(int transactionId, Handler<Message<JsonObject>> handler) {
		neo4j.rollbackTransaction(transactionId, handler);
	}

}