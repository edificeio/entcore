/*
 * Copyright © WebServices pour l'Éducation, 2015
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

package org.entcore.common.service.impl;

import com.mongodb.QueryBuilder;
import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.mongodb.MongoQueryBuilder;
import fr.wseduc.mongodb.MongoUpdateBuilder;
import org.entcore.common.mongodb.MongoDbConf;
import org.entcore.common.user.RepositoryEvents;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public abstract class MongoDbRepositoryEvents implements RepositoryEvents {

	protected static final Logger log = LoggerFactory.getLogger(MongoDbRepositoryEvents.class);
	protected final MongoDb mongo = MongoDb.getInstance();
	protected final String managerRight;
	protected final String revisionsCollection;
	protected final String revisionIdAttribute;

	protected MongoDbRepositoryEvents() {
		this(null, null, null);
	}

	protected MongoDbRepositoryEvents(String managerRight) {
		this(managerRight, null, null);
	}

	protected MongoDbRepositoryEvents(String managerRight, String revisionsCollection, String revisionIdAttribute) {
		this.managerRight = managerRight;
		this.revisionsCollection = revisionsCollection;
		this.revisionIdAttribute = revisionIdAttribute;
	}

	@Override
	public void deleteGroups(JsonArray groups) {
		if(groups == null || groups.size() == 0) {
			return;
		}

		final String[] groupIds = new String[groups.size()];
		for (int i = 0; i < groups.size(); i++) {
			JsonObject j = groups.getJsonObject(i);
			groupIds[i] = j.getString("group");
		}

		final JsonObject matcher = MongoQueryBuilder.build(QueryBuilder.start("shared.groupId").in(groupIds));

		MongoUpdateBuilder modifier = new MongoUpdateBuilder();
		modifier.pull("shared", MongoQueryBuilder.build(QueryBuilder.start("groupId").in(groupIds)));

		final String collection = MongoDbConf.getInstance().getCollection();
		if (collection == null || collection.trim().isEmpty()) {
			log.error("Error deleting groups : invalid collection " + collection + " in class " + this.getClass().getName());
			return;
		}
		mongo.update(collection, matcher, modifier.build(), false, true, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				if (!"ok".equals(event.body().getString("status"))) {
					log.error("Error deleting groups in collection " + collection +
							" : " + event.body().getString("message"));
				}
			}
		});
	}

	@Override
	public void deleteUsers(JsonArray users) {
		if(users == null || users.size() == 0) {
			return;
		}

		final String[] userIds = new String[users.size()];
		for (int i = 0; i < users.size(); i++) {
			JsonObject j = users.getJsonObject(i);
			userIds[i] = j.getString("id");
		}

		final JsonObject criteria = MongoQueryBuilder.build(QueryBuilder.start("shared.userId").in(userIds));

		MongoUpdateBuilder modifier = new MongoUpdateBuilder();
		modifier.pull("shared", MongoQueryBuilder.build(QueryBuilder.start("userId").in(userIds)));

		final String collection = MongoDbConf.getInstance().getCollection();
		if (collection == null || collection.trim().isEmpty()) {
			log.error("Error deleting groups : invalid collection " + collection + " in class " + this.getClass().getName());
			return;
		}
		mongo.update(collection, criteria, modifier.build(), false, true, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				if (!"ok".equals(event.body().getString("status"))) {
					log.error("Error deleting users shared in collection " + collection  +
							" : " + event.body().getString("message"));
				}

				final JsonObject criteria = MongoQueryBuilder.build(QueryBuilder.start("owner.userId").in(userIds));
				MongoUpdateBuilder modifier = new MongoUpdateBuilder();
				modifier.set("owner.deleted", true);
				mongo.update(collection, criteria, modifier.build(), false, true,  new Handler<Message<JsonObject>>() {
					@Override
					public void handle(Message<JsonObject> event) {
						if (!"ok".equals(event.body().getString("status"))) {
							log.error("Error deleting users shared in collection " + collection +
									" : " + event.body().getString("message"));
						} else if (managerRight != null && !managerRight.trim().isEmpty()) {
							removeObjects(collection);
						}
					}
				});
			}
		});
	}

	protected void removeObjects(final String collection) {
		JsonObject matcher = MongoQueryBuilder.build(
				QueryBuilder.start("shared." + managerRight).notEquals(true).put("owner.deleted").is(true));

		JsonObject projection = new JsonObject().put("_id", 1);

		// Get ids of objects who have no manager and no owner (owner has just been deleted, or has been deleted previously)
		mongo.find(collection, matcher, null, projection, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				JsonArray res = event.body().getJsonArray("results");
				if (!"ok".equals(event.body().getString("status"))) {
					log.error("Error when finding objects who have no manager and no owner : " +
							event.body().getString("message"));
				} else if (res == null || res.size() == 0) {
					log.info("There are no objects without manager and without owner : no objects to delete");
				} else {
					final String[] objectIds = new String[res.size()];
					for (int i = 0; i < res.size(); i++) {
						JsonObject j = res.getJsonObject(i);
						objectIds[i] = j.getString("_id");
					}
					JsonObject matcher = MongoQueryBuilder.build(QueryBuilder.start("_id").in(objectIds));
					mongo.delete(collection, matcher, new Handler<Message<JsonObject>>() {
						@Override
						public void handle(Message<JsonObject> event) {
							if (!"ok".equals(event.body().getString("status"))) {
								log.error("Error deleting objects in collection " + collection +
										" : " + event.body().getString("message"));
							} else if (revisionsCollection != null && !revisionsCollection.trim().isEmpty() &&
									revisionIdAttribute != null && !revisionIdAttribute.trim().isEmpty()) {
								JsonObject criteria = MongoQueryBuilder.build(
										QueryBuilder.start(revisionIdAttribute).in(objectIds));
								mongo.delete(revisionsCollection, criteria, new Handler<Message<JsonObject>>() {
									@Override
									public void handle(Message<JsonObject> event) {
										if (!"ok".equals(event.body().getString("status"))) {
											log.error("Error deleting revisions objects in collection " +
													revisionsCollection + " : " + event.body().getString("message"));
										}
									}
								});
							}
						}
					});
				}
			}
		});
	}

}
