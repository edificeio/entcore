/*
 * Copyright © "Open Digital Education", 2015
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

package org.entcore.common.service.impl;

import com.mongodb.QueryBuilder;
import fr.wseduc.mongodb.MongoQueryBuilder;
import fr.wseduc.mongodb.MongoUpdateBuilder;
import org.entcore.common.mongodb.MongoDbConf;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class MongoDbRepositoryEvents extends AbstractRepositoryEvents {

	protected static final Logger log = LoggerFactory.getLogger(MongoDbRepositoryEvents.class);
	protected final String managerRight;
	protected final String revisionsCollection;
	protected final String revisionIdAttribute;

	public MongoDbRepositoryEvents() {
		this(null, null, null, null);
	}

	public MongoDbRepositoryEvents(String managerRight) {
		this(null, managerRight, null, null);
	}

	public MongoDbRepositoryEvents(Vertx vertx) {
		this(vertx, null, null, null);
	}

	public MongoDbRepositoryEvents(Vertx vertx, String managerRight, String revisionsCollection,
			String revisionIdAttribute) {
		super(vertx);
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

	protected void exportFiles(final JsonArray results, String exportPath, Set<String> usedFileName,
			final AtomicBoolean exported, final Handler<Boolean> handler) {
		if (results.isEmpty()) {
			exported.set(true);
			log.info(title + " exported successfully to : " + exportPath);
			handler.handle(exported.get());
		} else {
			JsonObject resources = results.getJsonObject(0);
			String fileId = resources.getString("_id");
			String fileName = resources.getString("title");
			if (fileName == null) {
				fileName = resources.getString("name");
			}
			if (fileName != null && fileName.contains("/")) {
				fileName = fileName.replaceAll("/", "-");
			}
			if (!usedFileName.add(fileName)) {
				fileName += "_" + fileId;
			}
			final String filePath = exportPath + File.separator + fileName;
			vertx.fileSystem().writeFile(filePath, resources.toBuffer(), new Handler<AsyncResult<Void>>() {
				@Override
				public void handle(AsyncResult<Void> event) {
					if (event.succeeded()) {
						results.remove(0);
						exportFiles(results, exportPath, usedFileName, exported, handler);
					} else {
						log.error(title + " : Could not write file " + filePath, event.cause());
						handler.handle(exported.get());
					}
				}
			});
		}
	}

	@Override
	public void exportResources(String exportId, String userId, JsonArray g, String exportPath, String locale,
			String host, Handler<Boolean> handler) {
			QueryBuilder findByAuthor = QueryBuilder.start("author.userId").is(userId);
			QueryBuilder findByOwner = QueryBuilder.start("owner.userId").is(userId);
			QueryBuilder findByAuthorOrOwner = QueryBuilder.start().or(findByAuthor.get(), findByOwner.get());
			QueryBuilder findByShared = QueryBuilder.start().or(QueryBuilder.start("shared.userId").is(userId).get(),
					QueryBuilder.start("shared.groupId").in(g).get());
			QueryBuilder findByAuthorOrOwnerOrShared = QueryBuilder.start().or(findByAuthorOrOwner.get(),
					findByShared.get());
			final JsonObject query = MongoQueryBuilder.build(findByAuthorOrOwnerOrShared);
			final AtomicBoolean exported = new AtomicBoolean(false);
			final String collection = MongoDbConf.getInstance().getCollection();
			mongo.find(collection, query, new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> event) {
					JsonArray results = event.body().getJsonArray("results");
					if ("ok".equals(event.body().getString("status")) && results != null) {
						createExportDirectory(exportPath, locale, new Handler<String>() {
							@Override
							public void handle(String path) {
								if (path != null) {
									exportDocumentsDependancies(results, path, new Handler<Boolean>() {
										@Override
										public void handle(Boolean bool) {
											if (bool) {
												exportFiles(results, path, new HashSet<String>(), exported, handler);
											} else {
												// Should never happen, export doesn't fail if docs export fail.
												handler.handle(exported.get());
											}
										}
									});
								} else {
									handler.handle(exported.get());
								}
							}
						});
					} else {
						log.error(title + " : Could not proceed query " + query.encode(),
								event.body().getString("message"));
						handler.handle(exported.get());
					}
				}
			});
	}

}
