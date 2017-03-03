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

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonElement;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

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
			JsonArray legacyIndexes = neo4jConfig.getArray("legacy-indexes");
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
			JsonObject body = new JsonObject().putString("name", j.getString("name"));
			body.putObject("config", new JsonObject()
					.putString("type", j.getString("type", "exact"))
					.putString("provider", "lucene"));
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
				.putString("query", query)
				.putObject("params", params);
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
									handler.handle(new JsonObject().putArray("result", transformJson(json)));
								} else {
									handler.handle(json);
								}
							} else {
								handler.handle(new JsonObject().putString("message",
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
					.putString("method", "POST")
					.putString("to", "/cypher")
					.putObject("body", (JsonObject) q)
					.putNumber("id", i++);
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
									out.add(new JsonObject().putArray("result",
											transformJson(qr.getObject("body", new JsonObject())))
											.putNumber("idx", qr.getNumber("id")));
								}
								handler.handle(new JsonObject().putArray("results", out));
							} else {
								handler.handle(new JsonObject().putString("message",
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
			sendRequest(uri, new JsonObject().putArray("statements", statements), new Handler<HttpClientResponse>() {
				@Override
				public void handle(final HttpClientResponse resp) {
					resp.bodyHandler(new Handler<Buffer>() {

						@Override
						public void handle(Buffer b) {
							logger.debug(b.toString());
							if (resp.statusCode() != 404 && resp.statusCode() != 500) {
								JsonObject json = new JsonObject(b.toString("UTF-8"));
								JsonArray results = json.getArray("results");
								if (json.getArray("errors", new JsonArray()).size() == 0 &&
										results != null) {
									JsonArray out = new JsonArray();
									for (Object o : results) {
										if (!(o instanceof JsonObject)) continue;
										out.add(transformJson((JsonObject) o));
									}
									json.putArray("results", out);
									String commit = json.getString("commit");
									if (commit != null) {
										String[] c = commit.split("/");
										if (c.length > 2) {
											json.putNumber("transactionId", Integer.parseInt(c[c.length - 2]));
										}
									}
									json.removeField("errors");
									handler.handle(json);
								} else {
									if (transactionId == null && commit && allowRetry && json.getArray("errors") != null && json.getArray("errors").size() > 0) {
										JsonArray errors = json.getArray("errors");
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
									handler.handle(new JsonObject().putString("message",
											json.getArray("errors", new JsonArray()).encode()));
								}
							} else {
								handler.handle(new JsonObject().putString("message",
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
								if (json.getArray("errors", new JsonArray()).size() == 0) {
									json.removeField("errors");
									handler.handle(json);
								} else {
									handler.handle(new JsonObject().putString("message",
											json.getArray("errors", new JsonArray()).encode()));
								}
							} else {
								handler.handle(new JsonObject().putString("message", resp.statusMessage()));
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
			HttpClientRequest req = nodeManager.getClient().request(method, uri,
					new Handler<HttpClientResponse>() {
				@Override
				public void handle(final HttpClientResponse response) {
					response.bodyHandler(new Handler<Buffer>() {
						@Override
						public void handle(Buffer buffer) {
							if (response.statusCode() <= 200 && response.statusCode() < 300) {
								handler.handle(new JsonObject().putString("result", buffer.toString()));
							} else {
								handler.handle((new JsonObject().putString("message",
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
		final JsonArray columns = json.getArray("columns");
		final JsonArray data = json.getArray("data");
		final JsonArray out = new JsonArray();

		if (data != null && columns != null) {
			for (Object r: data) {
				JsonArray row;
				if (r instanceof JsonArray) {
					row = (JsonArray) r;
				} else if (r instanceof JsonObject) {
					row = ((JsonObject) r).getArray("row");
				} else {
					continue;
				}
				JsonObject outRow = new JsonObject();
				out.addObject(outRow);
				for (int j = 0; j < row.size(); j++) {
					Object value = row.get(j);
					if (value == null) {
						outRow.putValue((String) columns.get(j), null);
					} else if (value instanceof String) {
						outRow.putString((String) columns.get(j), (String) value);
					} else if (value instanceof JsonArray) {
						outRow.putArray((String) columns.get(j), (JsonArray) value);
					} else if (value instanceof JsonObject) {
						outRow.putObject((String) columns.get(j), (JsonObject) value);
					} else if (value instanceof Boolean) {
						outRow.putBoolean((String) columns.get(j), (Boolean) value);
					} else if (value instanceof Number) {
						outRow.putNumber((String) columns.get(j), (Number) value);
					} else {
						outRow.putString((String) columns.get(j), value.toString());
					}
				}
			}
		}
		return out;
	}

	private void sendRequest(String path, JsonElement body, final Handler<HttpClientResponse> handler)
			throws Neo4jConnectionException {
		sendRequest(path, body, false, handler);
	}

	private void sendRequest(String path, JsonElement body, boolean checkReadOnly,
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

		if (body.isArray()) {
			req.end(body.asArray().encode());
		} else {
			req.end(body.asObject().encode());
		}
	}

}
