/*
 * Copyright © "Open Digital Education", 2016
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

package org.entcore.feeder.dictionary.structures;

import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.*;
import org.entcore.common.appregistry.ApplicationUtils;
import org.entcore.common.events.EventStore;
import org.entcore.common.events.EventStoreFactory;
import org.entcore.common.user.UserInfos;
import org.entcore.feeder.Feeder;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.feeder.exceptions.TransactionException;
import org.entcore.feeder.utils.TransactionHelper;
import org.entcore.feeder.utils.TransactionManager;
import io.vertx.core.*;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;


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
		execute(null);
	}

	public void execute(String source) {
		storeImportedEvent();
		if (source == null || config.getJsonArray("exclude-mark-duplicates-by-source") == null ||
				!config.getJsonArray("exclude-mark-duplicates-by-source").contains(source)) {
			duplicateUsers.markDuplicates(new Handler<JsonObject>() {
				@Override
				public void handle(final JsonObject event) {
					duplicateUsers.autoMergeDuplicatesInStructure(new Handler<AsyncResult<JsonArray>>() {
						@Override
						public void handle(AsyncResult<JsonArray> mergedUsers) {
							applyComRules(getFinalHandler(source));
						}
					});
				}
			});
		} else {
			applyComRules(getFinalHandler(source));
		}
	}

	private Handler<Void> getFinalHandler(String source) {
		return new Handler<Void>() {
			@Override
			public void handle(Void v) {
				if (config.getBoolean("notify-apps-after-import", true)) {
					ApplicationUtils.afterImport(eb);
				}
				if (config.getJsonObject("ws-call-after-import") != null) {
					wsCall(config.getJsonObject("ws-call-after-import"));
				}
				if (config.getJsonArray("publish-classes-update") != null &&
						config.getJsonArray("publish-classes-update").contains(source)) {
					publishClassesUpdate();
				}
			}
		};
	}

	private void publishClassesUpdate() {
		final String query =
				"MATCH (u:User) " +
				"WHERE (has(u.classes) AND not(has(u.oldClasses))) OR u.classes <> u.oldClasses " +
				"RETURN u.id as userId, u.externalId as userExternalId, u.classes as classes, " +
				"u.oldClasses as oldClasses, u.created as created, timestamp() as timestamp";
		final String setOldClasses =
				"MATCH (u:User) " +
				"WHERE has(u.classes) " +
				"SET u.oldClasses = u.classes ";
		try {
			final TransactionHelper tx = TransactionManager.getTransaction();
			final JsonObject params = new JsonObject();
			tx.add(query, params);
			tx.add(setOldClasses, params);
			tx.commit(res -> {
				if ("ok".equals(res.body().getString("status"))) {
					JsonArray r = res.body().getJsonArray("results");
					if (r != null && r.getJsonArray(0) != null && r.getJsonArray(0).size() > 0) {
						eb.publish(Feeder.USER_REPOSITORY, new JsonObject()
								.put("action", "users-classes-update")
								.put("users-classes-update", r.getJsonArray(0)));
					}
				} else {
					logger.error("Error in publish classes update transaction : " + res.body().getString("message"));
				}
			});
		} catch (TransactionException e) {
			logger.error("Error in publish classes update transaction.", e);
		}
	}

	private void wsCall(JsonObject object) {
		for (String url : object.fieldNames()) {
			final JsonArray endpoints = object.getJsonArray(url);
			if (endpoints == null || endpoints.size() < 1) continue;
			try {
				final URI uri = new URI(url);
				HttpClientOptions options = new HttpClientOptions()
						.setDefaultHost(uri.getHost())
						.setDefaultPort(uri.getPort()).setMaxPoolSize(16)
						.setSsl("https".equals(uri.getScheme()))
						.setConnectTimeout(10000)
						.setKeepAlive(false);
				final HttpClient client = vertx.createHttpClient(options);

				final Handler[] handlers = new Handler[endpoints.size() + 1];
				handlers[handlers.length -1] = new Handler<Void>() {
					@Override
					public void handle(Void v) {
						client.close();
					}
				};
				for (int i = endpoints.size() - 1; i >= 0; i--) {
					final int ji = i;
					handlers[i] = new Handler<Void>() {
						@Override
						public void handle(Void v) {
							final JsonObject j = endpoints.getJsonObject(ji);
							logger.info("endpoint : " + j.encode());
							final HttpClientRequest req = client.request(HttpMethod.valueOf(j.getString("method")), j.getString("uri"), new Handler<HttpClientResponse>() {
								@Override
								public void handle(HttpClientResponse resp) {
									if (resp.statusCode() >= 300) {
										logger.warn("Endpoint " + j.encode() + " error : " + resp.statusCode() + " " + resp.statusMessage());
									}
									handlers[ji + 1].handle(null);
								}
							});
							JsonObject headers = j.getJsonObject("headers");
							if (headers != null && headers.size() > 0) {
								for (String h : headers.fieldNames()) {
									req.putHeader(h, headers.getString(h));
								}
							}
							req.exceptionHandler(e -> logger.error("Error in ws call post import : " + j.encode(), e));
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
				JsonArray res = event.body().getJsonArray("result");
				if ("ok".equals(event.body().getString("status")) && res != null && res.size() == 1) {
					eventStore.createAndStoreEvent(Feeder.FeederEvent.IMPORT.name(),
							(UserInfos) null, res.getJsonObject(0));
				} else {
					logger.error(event.body().getString("message"));
				}
			}
		});
	}

	private void applyComRules(final Handler<Void> handler) {
		if (config.getBoolean("apply-communication-rules", false)) {
			String q = "MATCH (s:Structure) return COLLECT(s.id) as ids";
			neo4j.execute(q, new JsonObject(), new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> message) {
					JsonArray ids = message.body().getJsonArray("result", new fr.wseduc.webutils.collections.JsonArray());
					if ("ok".equals(message.body().getString("status")) && ids != null &&
							ids.size() == 1) {
						JsonObject j = new JsonObject()
								.put("action", "initAndApplyDefaultCommunicationRules")
								.put("schoolIds", (ids.getJsonObject(0))
										.getJsonArray("ids", new fr.wseduc.webutils.collections.JsonArray()));
						eb.send("wse.communication", j, new DeliveryOptions().setSendTimeout(3600 * 1000l),
								handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
							@Override
							public void handle(Message<JsonObject> event) {
								if (!"ok".equals(event.body().getString("status"))) {
									logger.error("Init rules error : " + event.body().getString("message"));
								} else {
									logger.info("Communication rules applied.");
								}
								handler.handle(null);
							}
						}));
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
