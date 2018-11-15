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

package org.entcore.workspace.service.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.entcore.common.folders.ElementQuery;
import org.entcore.common.folders.FolderManager;
import org.entcore.common.storage.Storage;
import org.entcore.common.user.RepositoryEvents;
import org.entcore.workspace.controllers.WorkspaceController;
import org.entcore.workspace.dao.DocumentDao;

import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;

import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.mongodb.MongoQueryBuilder;
import fr.wseduc.mongodb.MongoUpdateBuilder;
import fr.wseduc.webutils.I18n;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class WorkspaceRepositoryEvents implements RepositoryEvents {

	private static final Logger log = LoggerFactory.getLogger(WorkspaceRepositoryEvents.class);
	private final MongoDb mongo = MongoDb.getInstance();
	private final Storage storage;
	private final FolderManager folderManager;
	private final FolderManager folderManagerRevision;
	private final Vertx vertx;
	private final boolean shareOldGroupsToUsers;

	public WorkspaceRepositoryEvents(Vertx vertx, Storage storage, boolean shareOldGroupsToUsers,
			FolderManager folderManager, FolderManager folderManagerRevision) {
		this.shareOldGroupsToUsers = shareOldGroupsToUsers;
		this.storage = storage;
		this.vertx = vertx;
		this.folderManager = folderManager;
		this.folderManagerRevision = folderManagerRevision;
	}

	@Override
	public void exportResources(final String exportId, final String userId, JsonArray g, final String exportPath,
			final String locale, String host, final Handler<Boolean> handler) {
		log.debug("Workspace export resources.");
		List<DBObject> groups = new ArrayList<>();
		groups.add(QueryBuilder.start("userId").is(userId).get());
		for (Object o : g) {
			if (!(o instanceof String))
				continue;
			String gpId = (String) o;
			groups.add(QueryBuilder.start("groupId").is(gpId).get());
		}
		QueryBuilder b = new QueryBuilder().or(QueryBuilder.start("owner").is(userId).get(),
				QueryBuilder.start("shared")
						.elemMatch(new QueryBuilder().or(groups.toArray(new DBObject[groups.size()])).get()).get(),
				QueryBuilder.start("old_shared")
						.elemMatch(new QueryBuilder().or(groups.toArray(new DBObject[groups.size()])).get()).get())
				.put("file").exists(true);
		final JsonObject keys = new JsonObject().put("file", 1).put("name", 1);
		final JsonObject query = MongoQueryBuilder.build(b);
		mongo.find(DocumentDao.DOCUMENTS_COLLECTION, query, null, keys, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				final AtomicBoolean exported = new AtomicBoolean(false);
				final JsonArray documents = event.body().getJsonArray("results");
				if ("ok".equals(event.body().getString("status")) && documents != null) {
					final Set<String> usedFileName = new HashSet<>();
					final JsonObject alias = new JsonObject();
					final String[] ids = new String[documents.size()];
					for (int i = 0; i < documents.size(); i++) {
						JsonObject j = documents.getJsonObject(i);
						ids[i] = j.getString("file");
						String fileName = j.getString("name");
						if (fileName != null && fileName.contains("/")) {
							fileName = fileName.replaceAll("/", "-");
						}
						if (usedFileName.add(fileName)) {
							alias.put(ids[i], fileName);
						} else {
							alias.put(ids[i], ids[i] + "_" + fileName);
						}
					}
					exportFiles(alias, ids, exportPath, locale, exported, handler);
				} else {
					log.error("Documents " + query.encode() + " - " + event.body().getString("message"));
					handler.handle(exported.get());
				}
			}
		});
	}

	private void exportFiles(final JsonObject alias, final String[] ids, String exportPath, String locale,
			final AtomicBoolean exported, final Handler<Boolean> handler) {
		createExportDirectory(exportPath, locale, new Handler<String>() {
			@Override
			public void handle(String path) {
				if (path != null) {
					if (ids.length == 0) {
						handler.handle(true);
						return;
					}
					storage.writeToFileSystem(ids, path, alias, new Handler<JsonObject>() {
						@Override
						public void handle(JsonObject event) {
							if ("ok".equals(event.getString("status"))) {
								exported.set(true);
								handler.handle(exported.get());
							} else {
								JsonArray errors = event.getJsonArray("errors",
										new fr.wseduc.webutils.collections.JsonArray());
								boolean ignoreErrors = errors.size() > 0;
								for (Object o : errors) {
									if (!(o instanceof JsonObject))
										continue;
									if (((JsonObject) o).getString("message") == null
											|| (!((JsonObject) o).getString("message").contains("NoSuchFileException")
													&& !((JsonObject) o).getString("message")
															.contains("FileAlreadyExistsException"))) {
										ignoreErrors = false;
										break;
									}
								}
								if (ignoreErrors) {
									exported.set(true);
									handler.handle(exported.get());
								} else {
									log.error("Write to fs : "
											+ new fr.wseduc.webutils.collections.JsonArray(Arrays.asList(ids)).encode()
											+ " - " + event.encode());
									handler.handle(exported.get());
								}
							}
						}
					});
				} else {
					log.error("Create export directory error.");
					handler.handle(exported.get());
				}
			}
		});
	}

	private void createExportDirectory(String exportPath, String locale, final Handler<String> handler) {
		final String path = exportPath + File.separator
				+ I18n.getInstance().translate("workspace.title", I18n.DEFAULT_DOMAIN, locale);
		vertx.fileSystem().mkdir(path, new Handler<AsyncResult<Void>>() {
			@Override
			public void handle(AsyncResult<Void> event) {
				if (event.succeeded()) {
					handler.handle(path);
				} else {
					handler.handle(null);
				}
			}
		});
	}

	@Override
	public void deleteGroups(JsonArray groups) {
		for (Object o : groups) {
			if (!(o instanceof JsonObject))
				continue;
			final JsonObject j = (JsonObject) o;
			final JsonObject query = MongoQueryBuilder
					.build(QueryBuilder.start("inheritedShares.groupId").is(j.getString("group")));
			final Handler<Message<JsonObject>> handler = new Handler<Message<JsonObject>>() {
				@Override
				public void handle(Message<JsonObject> event) {
					if (!"ok".equals(event.body().getString("status"))) {
						log.error("Error updating documents with group " + j.getString("group") + " : "
								+ event.body().encode());
					} else {
						log.info("Documents with group " + j.getString("group") + " updated : "
								+ event.body().getInteger("number"));
					}
				}
			};
			// REMOVE GROUPID from SHARED and INHERITSHARED
			final MongoUpdateBuilder update = new MongoUpdateBuilder()
					.pull("shared", new JsonObject().put("groupId", j.getString("group")))
					.pull("inheritedShares", new JsonObject().put("groupId", j.getString("group")))
					.addToSet("old_shared", new JsonObject().put("groupId", j.getString("group")));
			if (shareOldGroupsToUsers) {
				JsonArray userShare = new fr.wseduc.webutils.collections.JsonArray();
				for (Object u : j.getJsonArray("users")) {
					JsonObject share = new JsonObject().put("userId", u.toString())
							.put(WorkspaceController.GET_ACTION, true).put(WorkspaceController.COPY_ACTION, true);
					userShare.add(share);
				}
				// ADD userIds to shared and inheritshared
				update.addToSet("shared", new JsonObject().put("$each", userShare));
				update.addToSet("inheritedShares", new JsonObject().put("$each", userShare));
				mongo.update(DocumentDao.DOCUMENTS_COLLECTION, query, update.build(), false, true, handler);
			} else {
				mongo.update(DocumentDao.DOCUMENTS_COLLECTION, query, update.build(), false, true, handler);
			}
		}
	}

	@Override
	public void deleteUsers(JsonArray users) {
		Set<String> userIds = new HashSet<>();
		for (int i = 0; i < users.size(); i++) {
			JsonObject j = users.getJsonObject(i);
			String id = j.getString("id");
			userIds.add(id);
		}
		ElementQuery query = new ElementQuery(false);
		query.setOwnerIds(userIds);
		query.setActionNotExists(WorkspaceController.SHARED_ACTION);
		// remove file and folders recursively
		this.folderManager.deleteByQuery(query, Optional.empty(), e -> {
			if (!e.succeeded()) {
				log.error(e.cause(), "Failed to delete documents");
			}
		});
		// remove revision recursively
		this.folderManagerRevision.deleteByQuery(query, Optional.empty(), e -> {
			if (!e.succeeded()) {
				log.error(e.cause(), "Failed to delete revision");
			}
		});
		// remove from share
		this.removeFromInheritshares(userIds);
	}

	private void removeFromInheritshares(Collection<String> userIds) {
		final JsonObject query = MongoQueryBuilder.build(QueryBuilder.start("shared.userId").in(userIds));
		JsonObject update = new JsonObject()
				.put("$pull",
						new JsonObject().put("shared",
								MongoQueryBuilder.build(QueryBuilder.start("userId").in(userIds))))//
				.put("$pull", new JsonObject().put("inheritedShares",
						MongoQueryBuilder.build(QueryBuilder.start("userId").in(userIds))));

		mongo.update(DocumentDao.DOCUMENTS_COLLECTION, query, update, false, true, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				if (!"ok".equals(event.body().getString("status"))) {
					log.error(event.body().getString("message"));
				}
			}
		});
	}
}
