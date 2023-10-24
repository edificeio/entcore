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
import org.entcore.feeder.FeederLogger;
import org.entcore.feeder.exceptions.TransactionException;
import org.entcore.common.neo4j.TransactionHelper;
import org.entcore.feeder.utils.TransactionManager;
import io.vertx.core.*;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;


public class PostImport {

	private final EventBus eb;
	private final EventStore eventStore;
	private final DuplicateUsers duplicateUsers;
	private final Neo4j neo4j = TransactionManager.getNeo4jHelper();
	private final JsonObject config;
	private final Vertx vertx;
	private final JsonArray manualGroupLinkUsersAutoSources;
	private final JsonArray fixIncorrectStoragesSources;
	private final JsonArray tenantLinkStructureSources;
	protected final FeederLogger logger = new FeederLogger(e -> "PostImport");

	public PostImport(Vertx vertx, DuplicateUsers duplicateUsers, JsonObject config) {
		this.vertx = vertx;
		this.eb = vertx.eventBus();
		eventStore = EventStoreFactory.getFactory().getEventStore(Feeder.class.getSimpleName());
		this.duplicateUsers = duplicateUsers;
		this.config = config;
		this.manualGroupLinkUsersAutoSources = config
				.getJsonArray("manual-group-link-users-auto-sources", new JsonArray().add("AAF"));
		this.fixIncorrectStoragesSources = config
				.getJsonArray("fix-incorrect-storages-sources", new JsonArray().add("AAF"));
		this.tenantLinkStructureSources = config
				.getJsonArray("tenant-link-structure-sources", new JsonArray().add("AAF").add("AAF1D"));
	}

	public void execute() {
		execute(null);
	}

	public void execute(String source) {
		execute(source, null);
	}

	public void execute(String source, Set<String> structureExternalIds) {
		storeImportedEvent();
		if (source == null || config.getJsonArray("exclude-mark-duplicates-by-source") == null ||
				!config.getJsonArray("exclude-mark-duplicates-by-source").contains(source)) {
			logger.info(e-> "START mergeSameIne");
			duplicateUsers.mergeSameINE(config.getBoolean("execute-merge-ine", false), voidAsyncResult -> {
				if(voidAsyncResult.succeeded()){
					logger.info(e-> "SUCCEED mergeSameIne");
				}else{
					logger.error(e-> "FAILED mergeSameIne", voidAsyncResult.cause());
				}
				logger.info(e-> "START markDuplicates");
				duplicateUsers.markDuplicates(event -> {
					if(event != null && "ok".equals(event.getString("status"))){
						logger.info(e-> "SUCCEED markDuplicates");
					}else{
						logger.error(e-> "FAILED markDuplicates: "+ event);
					}
					logger.info(e-> "START autoMergeDuplicatesInStructure");
					duplicateUsers.autoMergeDuplicatesInStructure(mergedUsers -> {
							if(mergedUsers.succeeded()){
								logger.info(e-> "SUCCEED autoMergeDuplicatesInStructure");
							}else{
								logger.error(e-> "FAILED autoMergeDuplicatesInStructure", mergedUsers.cause());
							}
							applyComRules(getFinalHandler(source), structureExternalIds);
						});
					});
				});
		} else {
			logger.info(e-> "SKIP mergeSameIne");
			applyComRules(getFinalHandler(source), structureExternalIds);
		}
	}

	private Handler<Void> getFinalHandler(String source) {
		return new Handler<Void>() {
			@Override
			public void handle(Void v) {
				if (config.getJsonArray("active-user-from-old-platform") != null &&
						config.getJsonArray("active-user-from-old-platform").contains(source)) {
					logger.info(e-> "Start activeUserFromOldPlatform", true);
					User.searchUserFromOldPlatform(vertx);
				}
				if (config.getBoolean("notify-apps-after-import", true)) {
					logger.info(e-> "Start notifyAppsAfterImport", true);
					ApplicationUtils.afterImport(eb);
					logger.info(e-> "SUCCEED to notifyAppsAfterImport", true);
				}
				if (config.getJsonObject("ws-call-after-import") != null) {
					logger.info(e-> "Start notifyAppsAfterImport", true);
					wsCall(config.getJsonObject("ws-call-after-import"));
				}
				if (config.getJsonArray("publish-classes-update") != null &&
						config.getJsonArray("publish-classes-update").contains(source)) {
					logger.info(e-> "Start publishClassesUpdate", true);
					publishClassesUpdate();
				}
				if (Boolean.TRUE.equals(config.getBoolean("tenant-link-structure", true)) &&
					tenantLinkStructureSources.contains(source)) {
					logger.info(e-> "Start tenantLinkStructure", true);
					Tenant.linkStructures(eb);
				}
				if(Boolean.TRUE.equals(config.getBoolean("manual-group-link-users-auto", true)) &&
						manualGroupLinkUsersAutoSources.contains(source)) {
					logger.info(e-> "Start manualGroupLinkUsersAuto", true);
					Group.runLinkRules();
				}
				if(Boolean.TRUE.equals(config.getBoolean("fix-incorrect-storages", true)) &&
						fixIncorrectStoragesSources.contains(source)) {
					logger.info(e-> "Start fixIncorrectStorages", true);
					fixIncorrectStorages();
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
					logger.info(e-> "SUCCEED to publishClassesUpdate", true);
				} else {
					logger.error(t -> "Error in publish classes update transaction : " + res.body().getString("message"));
				}
			});
		} catch (TransactionException e) {
			logger.error(t -> "Error in publish classes update transaction.", e);
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
							logger.info(t -> "endpoint : " + j.encode(), true);
							client.request(HttpMethod.valueOf(j.getString("method")), j.getString("uri"))
							.map(req -> {
								JsonObject headers = j.getJsonObject("headers");
								if (headers != null && !headers.isEmpty()) {
									for (String h : headers.fieldNames()) {
										req.putHeader(h, headers.getString(h));
									}
								}
								return req;
							})
							.flatMap(req -> {
								if (j.getString("body") != null) {
									return req.send(j.getString("body"));
								} else {
									return req.send();
								}
							}).onSuccess(resp -> {
								if (resp.statusCode() >= 300) {
									logger.warn(t -> "Endpoint " + j.encode() + " error : " + resp.statusCode() + " " + resp.statusMessage());
								}
								handlers[ji + 1].handle(null);
							})
							.onFailure(e -> logger.error(t ->"Error in ws call post import : " + j.encode(), e));
						}
					};
				}
				handlers[0].handle(null);
			} catch (URISyntaxException e) {
				logger.error(t ->"Invalid uri in ws call after import : " + url, e);
			}
		}
		logger.info(e-> "SUCCEED to wsCallAfterImport", true);
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
					logger.error(t -> "FAILED storeImportedEvent "+event.body());
				}
			}
		});
	}

	private void applyComRules(final Handler<Void> handler, Set<String> structureExternalIds) {
		if (config.getBoolean("apply-communication-rules", false)) {
			logger.info(e-> "START get ids for applyComRules");
			String q = "MATCH (s:Structure) " + (structureExternalIds != null ? "WHERE s.externalId IN {externalIds}" : "") + " return COLLECT(s.id) as ids";
			JsonArray eIds = new JsonArray();
			if(structureExternalIds != null)
				for(String eId : structureExternalIds)
					eIds.add(eId);
			neo4j.execute(q, new JsonObject().put("externalIds", eIds), new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> message) {
					JsonArray ids = message.body().getJsonArray("result", new JsonArray());
					if ("ok".equals(message.body().getString("status")) && ids != null &&
							ids.size() == 1) {
						logger.info(e-> "SUCCEED get ids for applyComRules " + ids.getJsonObject(0).getJsonArray("ids"));
						JsonObject j = new JsonObject()
								.put("action", "initAndApplyDefaultCommunicationRules")
								.put("schoolIds", (ids.getJsonObject(0))
										.getJsonArray("ids", new JsonArray()));
						logger.info(e-> "START apply applyComRules");
						eb.request("wse.communication", j, new DeliveryOptions().setSendTimeout(3600 * 1000l),
								handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
							@Override
							public void handle(Message<JsonObject> event) {
								if (!"ok".equals(event.body().getString("status"))) {
									logger.error(e-> "FAILED to apply Communication rules"+ event.body().getString("message"));
								} else {
									logger.info(e-> "SUCCEED to apply Communication rules", true);
								}
								handler.handle(null);
							}
						}));
					} else {
						logger.error(e-> "FAILED get ids for applyComRules: "+message.body().getString("message"));
						handler.handle(null);
					}
				}
			});
		} else {
			logger.info(e-> "SKIP applyComRules");
			handler.handle(null);
		}
	}

	private void fixIncorrectStorages()
	{
		String setDefaultQuotas = "MATCH (p:Profile)-[:HAS_PROFILE]-(pg:ProfileGroup)-[:IN]-(u:User)-[r:USERBOOK]->(n:UserBook) " +
									"WHERE NOT(HAS(n.quota)) SET n.quota = p.defaultQuota";
		String setMissingStorages = "MATCH (n:UserBook) WHERE NOT(HAS(n.storage)) SET n.storage = 0";
		String fixNegativeStorages = "MATCH (n:UserBook) WHERE n.storage < 0 SET n.storage = 0";

		try {
			final TransactionHelper tx = TransactionManager.getTransaction();
			final JsonObject params = new JsonObject();
			tx.add(setDefaultQuotas, params);
			tx.add(setMissingStorages, params);
			tx.add(fixNegativeStorages, params);
			tx.commit(res -> {
				if ("ok".equals(res.body().getString("status"))) {
					logger.info(t -> "Incorrect storages have been fixed");
				} else {
					logger.error(t -> "Error in fixing incorrect storages transaction : " + res.body().getString("message"));
				}
				logger.info(e-> "SUCCEED to fixIncorrectStorages", true);
			});
		} catch (TransactionException e) {
			logger.error(t -> "Error in fixing incorrect storages transaction.", e);
		}
	}

}
