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

import fr.wseduc.webutils.Server;
import fr.wseduc.webutils.eventbus.ResultMessage;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.net.URI;
import java.util.Map;

public class Neo4j {

	private EventBus eb;
	private GraphDatabase database;
	private static final Logger log = LoggerFactory.getLogger(Neo4j.class);

	private Neo4j() {}

	private static class Neo4jHolder {
		private static final Neo4j instance = new Neo4j();
	}

	public static Neo4j getInstance() {
		return Neo4jHolder.instance;
	}

	/**
	 * Warning : use in some specifics case as two verticle with neo4j in same module.
	 * In others cases use getInstance() method.
	 */
	public static Neo4j getSpecificInstance() {
		return new Neo4j();
	}

	public void init(Vertx vertx, JsonObject config) {
		this.eb = Server.getEventBus(vertx);
		JsonArray serverUris = config.getJsonArray("server-uris");
		String serverUri = config.getString("server-uri");
		if (serverUris == null && serverUri != null) {
			serverUris = new fr.wseduc.webutils.collections.JsonArray().add(serverUri);
		}

		if (serverUris != null) {
			try {
				URI[] uris = new URI[serverUris.size()];
				for (int i = 0; i < serverUris.size(); i++) {
					uris[i] = new URI(serverUris.getString(i));
				}
				database = new Neo4jRest(uris, config.getBoolean("slave-readonly", false), vertx,
						config.getLong("checkDelay", 3000l),
						config.getInteger("poolSize", 16),
						config.getBoolean("keepAlive", true),
						config);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		} else {
			log.error("Invalid Neo4j URI");
		}
	}

	public void execute(String query, JsonObject params, Handler<Message<JsonObject>> handler) {
		database.execute(query, params, resultHandler(handler));
	}

	public void execute(String query, Map<String,Object> params, Handler<Message<JsonObject>> handler) {
		execute(query, params != null ? new JsonObject(params) : null, handler);
	}

	public void execute(String query, Map<String,Object> params, final HttpServerResponse response) {
		execute(query, params, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> m) {
				response.putHeader("Content-Type", "application/json");
				response.end(m.body().encode());
			}
		});
	}

	public void execute(String query, JsonObject params, final HttpServerResponse response) {
		execute(query, params, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> m) {
				response.putHeader("Content-Type", "application/json");
				response.end(m.body().encode());
			}
		});
	}

	public void executeBatch(JsonArray queries, final Handler<Message<JsonObject>> handler) {
		database.executeBatch(queries, resultHandler(handler));
	}

	public void executeBatch(JsonArray queries, final HttpServerResponse response) {
		executeBatch(queries, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> m) {
				response.putHeader("Content-Type", "application/json");
				response.end(m.body().encode());
			}
		});
	}

	public void executeTransaction(JsonArray statements, Integer transactionId, boolean commit,
			Handler<Message<JsonObject>> handler) {
		database.executeTransaction(statements, transactionId, commit, resultHandler(handler));
	}

	public void executeTransaction(JsonArray statements, Integer transactionId, boolean commit,
			boolean allowRetry, boolean forceReadOnly, Handler<Message<JsonObject>> handler) {
		database.executeTransaction(statements, transactionId, commit, allowRetry, forceReadOnly, resultHandler(handler));
	}

	public void resetTransactionTimeout(int transactionId, Handler<Message<JsonObject>> handler) {
		database.resetTransactionTimeout(transactionId, resultHandler(handler));
	}

	public void rollbackTransaction(int transactionId, Handler<Message<JsonObject>> handler) {
		database.rollbackTransaction(transactionId, resultHandler(handler));
	}

	public void unmanagedExtension(String method, String uri, String body, Handler<Message<JsonObject>> handler) {
		database.unmanagedExtension(method, uri, body, resultHandler(handler));
	}

	public EventBus getEventBus() {
		return eb;
	}

	private Handler<JsonObject> resultHandler(final Handler<Message<JsonObject>> m) {
		return new Handler<JsonObject>() {

			@Override
			public void handle(JsonObject res) {
				if (res.getString("message") != null) {
					log.error(res.getString("exception") + " : " + res.getString("message"));
					res.put("status", "error");
				}
				if (m != null) {
					m.handle(new ResultMessage(res));
				}
			}
		};
	}

}