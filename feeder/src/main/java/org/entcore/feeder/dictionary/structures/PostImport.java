/*
 * Copyright © WebServices pour l'Éducation, 2016
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

package org.entcore.feeder.dictionary.structures;

import org.entcore.common.appregistry.ApplicationUtils;
import org.entcore.common.events.EventStore;
import org.entcore.common.events.EventStoreFactory;
import org.entcore.common.user.UserInfos;
import org.entcore.feeder.Feeder;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.feeder.utils.TransactionManager;
import org.vertx.java.core.*;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;


public class PostImport {

	private static final Logger logger = LoggerFactory.getLogger(PostImport.class);
	private final EventBus eb;
	private final EventStore eventStore;
	private final DuplicateUsers duplicateUsers;
	private final Neo4j neo4j = TransactionManager.getNeo4jHelper();
	private final JsonObject config;
	private final Vertx vertx;

	public PostImport(Vertx vertx, DuplicateUsers duplicateUsers, JsonObject config) {
		this.vertx = vertx;
		this.eb = vertx.eventBus();
		eventStore = EventStoreFactory.getFactory().getEventStore(Feeder.class.getSimpleName());
		this.duplicateUsers = duplicateUsers;
		this.config = config;
	}

	public void execute() {
		storeImportedEvent();
		duplicateUsers.markDuplicates(new Handler<JsonObject>() {
			@Override
			public void handle(final JsonObject event) {
			duplicateUsers.autoMergeDuplicatesInStructure(new AsyncResultHandler<JsonArray>() {
				@Override
				public void handle(AsyncResult<JsonArray> mergedUsers) {
				applyComRules(new VoidHandler() {
					@Override
					protected void handle() {
					if (config.getBoolean("notify-apps-after-import", true)) {
						ApplicationUtils.afterImport(eb);
					}
					if (config.getObject("ws-call-after-import") != null) {
						wsCall(config.getObject("ws-call-after-import"));
					}
					}
				});
				}
			});
			}
		});
	}

	private void wsCall(JsonObject object) {
		for (String url : object.getFieldNames()) {
			final JsonArray endpoints = object.getArray(url);
			if (endpoints == null || endpoints.size() < 1) continue;
			try {
				final URI uri = new URI(url);
				final HttpClient client = vertx.createHttpClient().setHost(uri.getHost())
						.setPort(uri.getPort()).setMaxPoolSize(16)
						.setSSL("https".equals(uri.getScheme()))
						.setConnectTimeout(10000)
						.setKeepAlive(false);
				final VoidHandler[] handlers = new VoidHandler[endpoints.size() + 1];
				handlers[handlers.length -1] = new VoidHandler() {
					@Override
					protected void handle() {
						client.close();
					}
				};
				for (int i = endpoints.size() - 1; i >= 0; i--) {
					final int ji = i;
					handlers[i] = new VoidHandler() {
						@Override
						protected void handle() {
							final JsonObject j = endpoints.get(ji);
							logger.info("endpoint : " + j.encode());
							final HttpClientRequest req = client.request(j.getString("method"), j.getString("uri"), new Handler<HttpClientResponse>() {
								@Override
								public void handle(HttpClientResponse resp) {
									if (resp.statusCode() >= 300) {
										logger.warn("Endpoint " + j.encode() + " error : " + resp.statusCode() + " " + resp.statusMessage());
									}
									handlers[ji + 1].handle(null);
								}
							});
							JsonObject headers = j.getObject("headers");
							if (headers != null && headers.size() > 0) {
								for (String h : headers.getFieldNames()) {
									req.putHeader(h, headers.getString(h));
								}
							}
							if (j.getString("body") != null) {
								req.end(j.getString("body"));
							} else {
								req.end();
							}
						}
					};
				}
				handlers[0].handle(null);
			} catch (URISyntaxException e) {
				logger.error("Invalid uri in ws call after import : " + url, e);
			}
		}
	}

	private void storeImportedEvent() {
		String countQuery =
				"MATCH (:User) WITH count(*) as nbUsers " +
						"MATCH (:Structure) WITH count(*) as nbStructures, nbUsers " +
						"MATCH (:Class) RETURN nbUsers, nbStructures, count(*) as nbClasses ";
		neo4j.execute(countQuery, (JsonObject) null, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				JsonArray res = event.body().getArray("result");
				if ("ok".equals(event.body().getString("status")) && res != null && res.size() == 1) {
					eventStore.createAndStoreEvent(Feeder.FeederEvent.IMPORT.name(),
							(UserInfos) null, res.<JsonObject>get(0));
				} else {
					logger.error(event.body().getString("message"));
				}
			}
		});
	}

	private void applyComRules(final VoidHandler handler) {
		if (config.getBoolean("apply-communication-rules", false)) {
			String q = "MATCH (s:Structure) return COLLECT(s.id) as ids";
			neo4j.execute(q, new JsonObject(), new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> message) {
					JsonArray ids = message.body().getArray("result", new JsonArray());
					if ("ok".equals(message.body().getString("status")) && ids != null &&
							ids.size() == 1) {
						JsonObject j = new JsonObject()
								.putString("action", "initAndApplyDefaultCommunicationRules")
								.putArray("schoolIds", ((JsonObject) ids.get(0))
										.getArray("ids", new JsonArray()));
						eb.send("wse.communication", j, new Handler<Message<JsonObject>>() {
							@Override
							public void handle(Message<JsonObject> event) {
								if (!"ok".equals(event.body().getString("status"))) {
									logger.error("Init rules error : " + event.body().getString("message"));
								} else {
									logger.info("Communication rules applied.");
								}
								handler.handle(null);
							}
						});
					} else {
						logger.error(message.body().getString("message"));
						handler.handle(null);
					}
				}
			});
		} else {
			handler.handle(null);
		}
	}

}
