/*
 * Copyright © "Open Digital Education", 2017
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

package org.entcore.common.neo4j;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.utils.StringUtils;
import org.entcore.common.utils.ExceptionUtils;

import java.net.URI;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Neo4jRest implements GraphDatabase {
	private static final Logger logger = LoggerFactory.getLogger(Neo4jRest.class);
	private static final String EMPTY_STATEMENTS_STRING = "{\"statements\":[]}";

	private final Neo4jRestClientNodeManager nodeManager;
	private final boolean ro;
	private final String basePath;
	private final Pattern writingClausesPattern = Pattern.compile(
			"(\\s+set\\s+|create\\s+|merge\\s+|delete\\s+|remove\\s+|foreach)", Pattern.CASE_INSENSITIVE);
	private boolean ignoreEmptyStateError = false;
	private final String authorizationHeader;

	public Neo4jRest(URI[] uris, boolean ro, Vertx vertx, long checkDelay, int poolSize,
					 boolean keepAlive, JsonObject neo4jConfig) {
		//prepare authorization
		if (neo4jConfig != null) {
			if(neo4jConfig.containsKey("username") && neo4jConfig.containsKey("password")){
				final String userCredentials = neo4jConfig.getString("username") + ":" + neo4jConfig.getString("password");
				this.authorizationHeader = "Basic " + new String(Base64.getEncoder().encode(userCredentials.getBytes()));
			} else {
				this.authorizationHeader = null;
			}
		}else{
			this.authorizationHeader = null;
		}
		//
		nodeManager = new Neo4jRestClientNodeManager(uris, vertx, checkDelay, poolSize, keepAlive, authorizationHeader, neo4jConfig);
		this.ro = ro;
		String path = uris[0].getPath();
		if (path != null && path.endsWith("/")) {
			this.basePath  = path.substring(0, path.length() - 1);
		} else {
			this.basePath = path;
		}
		if (neo4jConfig != null) {
			JsonArray legacyIndexes = neo4jConfig.getJsonArray("legacy-indexes");
			if (legacyIndexes != null && legacyIndexes.size() > 0) {
				for (Object o : legacyIndexes) {
					if (!(o instanceof JsonObject)) continue;
					JsonObject j = (JsonObject) o;
					createIndex(j);
				}
			}
			ignoreEmptyStateError = neo4jConfig.getBoolean("ignore-empty-statements-error", false);
		}
	}

	public JsonObject getMetrics(){
		final long count = this.nodeManager.getClients().size();
		final long down = this.nodeManager.getClients().stream().filter(e->!e.isAvailable()).count();
		final long up = this.nodeManager.getClients().stream().filter(e->e.isAvailable()).count();
		return new JsonObject().put("neo4j_instance_up", up).put("neo4j_instance_down", down).put("neo4j_instance_total", count);
	}

	private HttpClientRequest prepareRequest(final HttpClientRequest request){
		if(!StringUtils.isEmpty(this.authorizationHeader)){
			request.headers().add("Authorization", this.authorizationHeader);
		}
		return request;
	}

	private void createIndex(final JsonObject j) {
		try {
			final JsonObject body = new JsonObject().put("name", j.getString("name"));
			body.put("config", new JsonObject()
					.put("type", j.getString("type", "exact"))
					.put("provider", "lucene"));
			nodeManager.getMasterClient().request(HttpMethod.POST, "/db/data/index/" + j.getString("for"))
					.map(this::prepareRequest)
					.flatMap(request -> request.send(body.encode()))
					.onFailure(th -> logger.error("Error creating index : " + j.encode(), th))
					.onSuccess(event -> {
					if (event.statusCode() != 201) {
						event.bodyHandler(new Handler<Buffer>() {
							@Override
							public void handle(Buffer event) {
								logger.error("Error creating index : " + j.encode() + " -> " + event.toString());
							}
						});
					}
			});
		} catch (Neo4jConnectionException e) {
			logger.error(e.getMessage(), e);
		}
	}

	@Override
	public void execute(String query, JsonObject p, final Handler<JsonObject> handler) {
		JsonObject params = p;
		if (params == null) {
			params = new JsonObject();
		}
		JsonObject body = new JsonObject()
				.put("query", query)
				.put("params", params);
		logger.debug(body.encode());
		try {
			sendRequest("/cypher", body, true, new Handler<HttpClientResponse>() {

				@Override
				public void handle(final HttpClientResponse resp) {
					if (resp == null) {
						handler.handle(new JsonObject().put("message", "Missing response from neo4j."));
					} else {
						resp.bodyHandler(b -> {
							logger.debug(b.toString());
							if (resp.statusCode() != 404 && resp.statusCode() != 500) {
								JsonObject json = new JsonObject(b.toString("UTF-8"));
								if (resp.statusCode() == 200) {
									handler.handle(new JsonObject().put("result", transformJson(json)));
								} else {
									handler.handle(json);
								}
							} else {
								handler.handle(new JsonObject().put("message",
										resp.statusMessage() + " : " + b.toString()));
							}
						});
					}
				}
			});
		} catch (Neo4jConnectionException e) {
			handler.handle(ExceptionUtils.exceptionToJson(e));
			logger.error("Neo4j execution failed", e);
		}
	}

	@Override
	public void executeBatch(JsonArray queries, final Handler<JsonObject> handler) {
		JsonArray body = new JsonArray();
		int i = 0;
		for (Object q : queries) {
			JsonObject query = new JsonObject()
					.put("method", "POST")
					.put("to", "/cypher")
					.put("body", (JsonObject) q)
					.put("id", i++);
			body.add(query);
		}
		logger.debug(body.encode());
		try {
			sendRequest("/batch", body, new Handler<HttpClientResponse>() {
				@Override
				public void handle(final HttpClientResponse resp) {
					resp.bodyHandler(new Handler<Buffer>() {

						@Override
						public void handle(Buffer b) {
							logger.debug(b.toString());
							if (resp.statusCode() != 404 && resp.statusCode() != 500) {
								JsonArray json = new JsonArray(b.toString("UTF-8"));
								JsonArray out = new JsonArray();
								for (Object j : json) {
									JsonObject qr = (JsonObject) j;
									out.add(new JsonObject().put("result",
											transformJson(qr.getJsonObject("body", new JsonObject())))
											.put("idx", qr.getLong("id")));
								}
								handler.handle(new JsonObject().put("results", out));
							} else {
								handler.handle(new JsonObject().put("message",
										resp.statusMessage() + " : " + b.toString()));
							}
						}
					});
				}
			});
		} catch (Neo4jConnectionException e) {
			ExceptionUtils.exceptionToJson(e);
		}
	}

	@Override
	public void executeTransaction(JsonArray statements, Integer transactionId,
								   boolean commit, final Handler<JsonObject> handler) {
		executeTransaction(statements, transactionId, commit, true, handler);
	}

	public void executeTransaction(final JsonArray statements, final Integer transactionId,
			final boolean commit, final boolean allowRetry, final Handler<JsonObject> handler) {
		executeTransaction(statements, transactionId, commit, allowRetry, false, handler);
	}

	@Override
	public void executeTransaction(final JsonArray statements, final Integer transactionId,
			final boolean commit, final boolean allowRetry, final boolean forceReadOnly,
			final Handler<JsonObject> handler) {
		// if (ignoreEmptyStateError && !commit && statements.isEmpty()) {
		// 	logger.warn("Ignore empty transaction call without commit. Transaction id : " + transactionId);
		// 	handler.handle(new JsonObject().put("results", new JsonArray()));
		// 	return;
		// }
		String uri = "/transaction";
		if (transactionId != null) {
			uri += "/" +transactionId;
		}
		if (commit) {
			uri += "/commit";
		}
		try {
			sendRequest(uri, new JsonObject().put("statements", statements), false, forceReadOnly,
					new Handler<HttpClientResponse>() {
				@Override
				public void handle(final HttpClientResponse resp) {
					if(resp == null) {
						logger.error("Received a null response from neo4J");
						handler.handle(new JsonObject().put("message", "no answer from server"));
					} else {
						resp.bodyHandler(new Handler<Buffer>() {

							@Override
							public void handle(Buffer b) {
								logger.debug(b.toString());
								if (resp.statusCode() != 404 && resp.statusCode() != 500) {
									JsonObject json = new JsonObject(b.toString("UTF-8"));
									JsonArray results = json.getJsonArray("results");
									if (json.getJsonArray("errors", new JsonArray()).size() == 0 &&
										results != null) {
										JsonArray out = new JsonArray();
										for (Object o : results) {
											if (!(o instanceof JsonObject)) continue;
											out.add(transformJson((JsonObject) o));
										}
										json.put("results", out);
										String commit = json.getString("commit");
										if (commit != null) {
											String[] c = commit.split("/");
											if (c.length > 2) {
												json.put("transactionId", Integer.parseInt(c[c.length - 2]));
											}
										}
										json.remove("errors");
										handler.handle(json);
									} else {
										if (transactionId == null && commit && allowRetry && json.getJsonArray("errors") != null && json.getJsonArray("errors").size() > 0) {
											JsonArray errors = json.getJsonArray("errors");
											for (Object o : errors) {
												if (!(o instanceof JsonObject)) continue;
												switch (((JsonObject) o).getString("code", "")) {
													case "Neo.TransientError.Transaction.ConstraintsChanged":
													case "Neo.TransientError.Transaction.DeadlockDetected":
													case "Neo.TransientError.Transaction.InstanceStateChanged":
													case "Neo.TransientError.Schema.SchemaModifiedConcurrently":
														executeTransaction(statements, transactionId, commit, false, handler);
														if (logger.isDebugEnabled()) {
															logger.debug("Retry transaction : " + statements.encode());
														}
														return;
												}
											}
										}
										handler.handle(new JsonObject().put("message",
											json.getJsonArray("errors", new JsonArray()).encode()));
									}
								} else {
									handler.handle(new JsonObject().put("message",
										resp.statusMessage() + " : " + b.toString()));
								}
							}
						});
					}
				}
			});
		} catch (Neo4jConnectionException e) {
			ExceptionUtils.exceptionToJson(e);
		}
	}

	@Override
	public void resetTransactionTimeout(int transactionId, Handler<JsonObject> handler) {
		executeTransaction(new JsonArray(), transactionId, false, handler);
	}

	@Override
	public void rollbackTransaction(int transactionId, final Handler<JsonObject> handler) {
		try {
			nodeManager.getMasterClient()
					.request(HttpMethod.DELETE, basePath + "/transaction/" + transactionId)
					.map(this::prepareRequest)
					.map(r -> r.putHeader("Accept", "application/json; charset=UTF-8"))
					.flatMap(HttpClientRequest::send)
					.onSuccess(resp -> {
						resp.bodyHandler(b -> {
							logger.debug(b.toString());
							if (resp.statusCode() != 404 && resp.statusCode() != 500) {
								JsonObject json = new JsonObject(b.toString("UTF-8"));
								if (json.getJsonArray("errors", new JsonArray()).isEmpty()) {
									json.remove("errors");
									handler.handle(json);
								} else {
									handler.handle(new JsonObject().put("message",
											json.getJsonArray("errors", new JsonArray()).encode()));
								}
							} else {
								handler.handle(new JsonObject().put("message", resp.statusMessage()));
							}
						});
					})
					.onFailure(e -> logger.error("Error rollbacking transaction : " + transactionId, e));
		} catch (Neo4jConnectionException e) {
			ExceptionUtils.exceptionToJson(e);
		}
	}

	@Override
	public void unmanagedExtension(String method, String uri, String body, final Handler<JsonObject> handler) {
		try {
			nodeManager.getMasterClient().request(HttpMethod.valueOf(method.toUpperCase()), uri)
					.map(this::prepareRequest)
					.flatMap(r -> body == null ? r.send() : r.send(body))
					.onSuccess(response ->  {
						response.bodyHandler(buffer -> {
							if (response.statusCode() <= 200 && response.statusCode() < 300) {
								handler.handle(new JsonObject().put("result", buffer.toString()));
							} else {
								handler.handle((new JsonObject().put("message",
										response.statusMessage()  + " : " + buffer.toString())));
							}
						});
					})
					.onFailure(e -> logger.error("Neo4j unmanaged extension error.", e));
		} catch (Neo4jConnectionException e) {
			ExceptionUtils.exceptionToJson(e);
		}
	}

	@Override
	public void close() {
		nodeManager.close();
	}

	private JsonArray transformJson(JsonObject json) {
		final JsonArray columns = json.getJsonArray("columns");
		final JsonArray data = json.getJsonArray("data");
		final JsonArray out = new JsonArray();

		if (data != null && columns != null) {
			for (Object r: data) {
				JsonArray row;
				if (r instanceof JsonArray) {
					row = (JsonArray) r;
				} else if (r instanceof JsonObject) {
					row = ((JsonObject) r).getJsonArray("row");
				} else {
					continue;
				}
				JsonObject outRow = new fr.wseduc.webutils.collections.JsonObject();
				out.add(outRow);
				for (int j = 0; j < row.size(); j++) {
					Object value = row.getValue(j);
					if (value == null) {
						outRow.put(columns.getString(j), (String) null);
					} else if (value instanceof String) {
						outRow.put(columns.getString(j), (String) value);
					} else if (value instanceof JsonArray) {
						outRow.put(columns.getString(j), (JsonArray) value);
					} else if (value instanceof JsonObject) {
						outRow.put(columns.getString(j), (JsonObject) value);
					} else if (value instanceof Boolean) {
						outRow.put(columns.getString(j), (Boolean) value);
					} else if (value instanceof Number) {
						outRow.put(columns.getString(j), (Number) value);
					} else {
						outRow.put(columns.getString(j), value.toString());
					}
				}
			}
		}
		return out;
	}

	private void sendRequest(String path, Object body, final Handler<HttpClientResponse> handler)
			throws Neo4jConnectionException {
		sendRequest(path, body, false, handler);
	}

	private void sendRequest(String path, Object body, boolean checkReadOnly,
			final Handler<HttpClientResponse> handler) throws Neo4jConnectionException {
		sendRequest(path, body, checkReadOnly, false, handler);
	}

	private void sendRequest(String path, Object body, boolean checkReadOnly, boolean forceReadOnly,
			final Handler<HttpClientResponse> handler) throws Neo4jConnectionException {
		sendRequest(path, body, checkReadOnly, forceReadOnly, 3, handler);
	}

	private void sendRequest(String path, Object body, boolean checkReadOnly, boolean forceReadOnly, int retry,
			final Handler<HttpClientResponse> handler) throws Neo4jConnectionException {
		HttpClient client = null;
		if (forceReadOnly && ro) {
			client = nodeManager.getSlaveClient();
		} else if (checkReadOnly && ro) {
			String query = ((JsonObject) body).getString("query");
			if (query != null) {
				Matcher m = writingClausesPattern.matcher(query);
				if (!m.find()) {
					client = nodeManager.getSlaveClient();
				}
			}
		}
		if (client == null) {
			client = nodeManager.getMasterClient();
		}
		final String b = Json.encode(body);
		client.request(HttpMethod.POST, basePath + path)
				.map(req -> req.putHeader("Content-Type", "application/json")
						.putHeader("Accept", "application/json; charset=UTF-8"))
				.map(this::prepareRequest)
				.flatMap(r -> r.send(b))
				.onSuccess(handler)
				.onFailure(event -> {
					logger.error("Neo4j error in request : " + path + " - " + b, event);
					if (ignoreEmptyStateError && EMPTY_STATEMENTS_STRING.equals(b) && retry > 0) {
						logger.warn("Retry sendRequest with empty statements.");
						try {
							sendRequest(path, body, checkReadOnly, forceReadOnly, (retry - 1), handler);
							return;
						} catch (Neo4jConnectionException e) {
							logger.error("Error when try retry sendRequest call.", e);
						}
						handler.handle(null);
					} else {
						handler.handle(null);
					}
				});

	}

}
