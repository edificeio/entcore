/*
 * Copyright © WebServices pour l'Éducation, 2017
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

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Neo4jRest implements GraphDatabase {

	private final Neo4jRestNodeClient nodeManager;
	private final boolean ro;
	private static final Logger logger = LoggerFactory.getLogger(Neo4jRest.class);
	private final String basePath;
	private Pattern writingClausesPattern = Pattern.compile(
			"(\\s+set\\s+|create\\s+|merge\\s+|delete\\s+|remove\\s+|foreach)", Pattern.CASE_INSENSITIVE);

	public Neo4jRest(URI[] uris, boolean ro, Vertx vertx, long checkDelay, int poolSize,
					 boolean keepAlive, JsonObject neo4jConfig) {
		nodeManager = new Neo4jRestNodeClient(uris, vertx, checkDelay, poolSize, keepAlive);
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
		}
	}

	private void createIndex(final JsonObject j) {
		try {
			final HttpClientRequest req = nodeManager.getClient()
					.post("/db/data/index/" + j.getString("for"), new Handler<HttpClientResponse>() {
				@Override
				public void handle(HttpClientResponse event) {
					if (event.statusCode() != 201) {
						event.bodyHandler(new Handler<Buffer>() {
							@Override
							public void handle(Buffer event) {
								logger.error("Error creating index : " + j.encode() + " -> " + event.toString());
							}
						});
					}
				}
			});
			JsonObject body = new JsonObject().put("name", j.getString("name"));
			body.put("config", new JsonObject()
					.put("type", j.getString("type", "exact"))
					.put("provider", "lucene"));
			req.end(body.encode());
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
					resp.bodyHandler(new Handler<Buffer>() {

						@Override
						public void handle(Buffer b) {
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
						}
					});
				}
			});
		} catch (Neo4jConnectionException e) {
			ExceptionUtils.exceptionToJson(e);
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
		String uri = "/transaction";
		if (transactionId != null) {
			uri += "/" +transactionId;
		}
		if (commit) {
			uri += "/commit";
		}
		try {
			sendRequest(uri, new JsonObject().put("statements", statements), new Handler<HttpClientResponse>() {
				@Override
				public void handle(final HttpClientResponse resp) {
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
			HttpClientRequest req = nodeManager.getClient().delete(
					basePath + "/transaction/" + transactionId, new Handler<HttpClientResponse>() {
				@Override
				public void handle(final HttpClientResponse resp) {
					resp.bodyHandler(new Handler<Buffer>() {

						@Override
						public void handle(Buffer b) {
							logger.debug(b.toString());
							if (resp.statusCode() != 404 && resp.statusCode() != 500) {
								JsonObject json = new JsonObject(b.toString("UTF-8"));
								if (json.getJsonArray("errors", new JsonArray()).size() == 0) {
									json.remove("errors");
									handler.handle(json);
								} else {
									handler.handle(new JsonObject().put("message",
											json.getJsonArray("errors", new JsonArray()).encode()));
								}
							} else {
								handler.handle(new JsonObject().put("message", resp.statusMessage()));
							}
						}
					});
				}
			});
			req.headers().add("Accept", "application/json; charset=UTF-8");
			req.end();
		} catch (Neo4jConnectionException e) {
			ExceptionUtils.exceptionToJson(e);
		}
	}

	@Override
	public void unmanagedExtension(String method, String uri, String body, final Handler<JsonObject> handler) {
		try {
			HttpClientRequest req = nodeManager.getClient().request(HttpMethod.valueOf(method.toUpperCase()), uri,
					new Handler<HttpClientResponse>() {
				@Override
				public void handle(final HttpClientResponse response) {
					response.bodyHandler(new Handler<Buffer>() {
						@Override
						public void handle(Buffer buffer) {
							if (response.statusCode() <= 200 && response.statusCode() < 300) {
								handler.handle(new JsonObject().put("result", buffer.toString()));
							} else {
								handler.handle((new JsonObject().put("message",
										response.statusMessage()  + " : " + buffer.toString())));
							}
						}
					});
				}
			});
			if (body != null) {
				req.end(body);
			} else {
				req.end();
			}
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
		HttpClient client = null;
		if (checkReadOnly && ro) {
			String query = ((JsonObject) body).getString("query");
			if (query != null) {
				Matcher m = writingClausesPattern.matcher(query);
				if (!m.find()) {
					client = nodeManager.getSlaveClient();
				}
			}
		}
		if (client == null) {
			client = nodeManager.getClient();
		}
		HttpClientRequest req = client.post(basePath + path, handler);
		req.headers()
				.add("Content-Type", "application/json")
				.add("Accept", "application/json; charset=UTF-8");

		req.end(Json.encode(body));
	}

}
