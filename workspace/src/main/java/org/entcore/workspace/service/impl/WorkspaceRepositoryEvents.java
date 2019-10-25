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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import io.vertx.core.AsyncResult;
import org.entcore.common.folders.ElementQuery;
import org.entcore.common.folders.FolderExporter;
import org.entcore.common.folders.FolderExporter.FolderExporterContext;
import org.entcore.common.folders.FolderImporter;
import org.entcore.common.folders.FolderImporter.FolderImporterContext;
import org.entcore.common.folders.FolderManager;
import org.entcore.common.storage.Storage;
import org.entcore.common.user.RepositoryEvents;
import org.entcore.common.user.UserInfos;
import org.entcore.common.utils.StringUtils;
import org.entcore.workspace.controllers.WorkspaceController;
import org.entcore.workspace.dao.DocumentDao;
import org.entcore.common.folders.impl.DocumentHelper;

import com.mongodb.QueryBuilder;

import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.mongodb.MongoQueryBuilder;
import fr.wseduc.mongodb.MongoUpdateBuilder;
import fr.wseduc.webutils.I18n;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class WorkspaceRepositoryEvents implements RepositoryEvents {

	private static final Logger log = LoggerFactory.getLogger(WorkspaceRepositoryEvents.class);
	private final MongoDb mongo = MongoDb.getInstance();
	private final FolderManager folderManager;
	private final FolderManager folderManagerRevision;
	private final boolean shareOldGroupsToUsers;
	private final FolderExporter exporter;
	private final FolderImporter importer;
	private final FileSystem fs;

	public WorkspaceRepositoryEvents(Vertx vertx, Storage storage, boolean shareOldGroupsToUsers,
			FolderManager folderManager, FolderManager folderManagerRevision) {
		this.shareOldGroupsToUsers = shareOldGroupsToUsers;
		this.folderManager = folderManager;
		this.folderManagerRevision = folderManagerRevision;
		this.exporter = new FolderExporter(storage, vertx.fileSystem(), false);
		this.importer = new FolderImporter(vertx.fileSystem(), vertx.eventBus(), false);
		this.fs = vertx.fileSystem();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void exportResources(final JsonArray resourcesIds, final String exportId, final String userId, JsonArray groupIds, final String exportPathOrig,
			final String locale, String host, final Handler<Boolean> handler) {

		QueryBuilder findByOwner = QueryBuilder.start("owner").is(userId);
		QueryBuilder findByShared = QueryBuilder.start().or(QueryBuilder.start("inheritedShares.userId").is(userId).get(),
				QueryBuilder.start("inheritedShares.groupId").in(groupIds).get());
		QueryBuilder findByOwnerOrShared = QueryBuilder.start().or(findByOwner.get(),
				findByShared.get());

		QueryBuilder findByOwnerOrSharedResourcesRestricted;

			if(resourcesIds == null)
				findByOwnerOrSharedResourcesRestricted = findByOwnerOrShared;
			else
			{
				findByOwnerOrSharedResourcesRestricted = findByOwnerOrShared.and(
					QueryBuilder.start("_id").in(resourcesIds).get()
				);
			}


		QueryBuilder isNotShared = QueryBuilder.start().or(
			QueryBuilder.start("isShared").exists(false).get(),
			QueryBuilder.start("isShared").is(false).get()
		);
		QueryBuilder isNotProtected = QueryBuilder.start().or(
			QueryBuilder.start("protected").exists(false).get(),
			QueryBuilder.start("protected").is(false).get()
		);
		QueryBuilder isNotPublic = QueryBuilder.start().or(
			QueryBuilder.start("public").exists(false).get(),
			QueryBuilder.start("public").is(false).get()
		);
		QueryBuilder isNotDeleted = QueryBuilder.start().or(
			QueryBuilder.start("deleted").exists(false).get(),
			QueryBuilder.start("deleted").is(false).get()
		);

		QueryBuilder myDocs = QueryBuilder.start().and(isNotShared.get(),isNotProtected.get(),isNotPublic.get(),isNotDeleted.get());
		QueryBuilder sharedDocs = QueryBuilder.start("isShared").is(true).and(isNotDeleted.get(),isNotProtected.get(),isNotPublic.get());
		QueryBuilder protectedDocs = QueryBuilder.start("protected").is(true).and(isNotDeleted.get());
		QueryBuilder trashDocs = QueryBuilder.start("deleted").is(true).and("trasher").is(userId);

		final JsonObject queryMyDocs = MongoQueryBuilder.build(QueryBuilder.start().and(findByOwnerOrSharedResourcesRestricted.get(),myDocs.get()));
		final JsonObject querySharedDocs = MongoQueryBuilder.build(QueryBuilder.start().and(findByOwnerOrSharedResourcesRestricted.get(),sharedDocs.get()));
		final JsonObject queryProtectedDocs = MongoQueryBuilder.build(QueryBuilder.start().and(findByOwnerOrSharedResourcesRestricted.get(),protectedDocs.get()));
		final JsonObject queryTrashDocs = MongoQueryBuilder.build(QueryBuilder.start().and(findByOwnerOrSharedResourcesRestricted.get(),trashDocs.get()));

		final Map<String,JsonObject> queries = new HashMap<>();
		queries.put("documents",queryMyDocs);
		queries.put("shared",querySharedDocs);
		queries.put("appDocuments",queryProtectedDocs);
		queries.put("trash",queryTrashDocs);

		final String exportPath = exportPathOrig + File.separator + "Documents_tmp";
		final String finalExportPath = exportPathOrig + File.separator +
				I18n.getInstance().translate("workspace.title", I18n.DEFAULT_DOMAIN, locale);

		exportDocs(exportPath, locale, queries, new JsonArray(), new Handler<Boolean>() {
			@Override
			public void handle(Boolean bool) {
				if (bool.booleanValue()) {
					fs.move(exportPath, finalExportPath, new Handler<AsyncResult<Void>>() {
						@Override
						public void handle(AsyncResult<Void> event) {
							if (event.succeeded()) {
								log.info("Documents exported successfully to : " + finalExportPath);
								handler.handle(true);
							} else {
								log.error("Documents : Failed to export documents to " + finalExportPath + " - " + event.cause());
								handler.handle(false);
							}
						}
					});
				} else {
					log.error("Documents : Failed to export documents to " + finalExportPath);
					handler.handle(false);
				}
			}
		});

	}

	private JsonArray removeDuplicatesAndErrors(JsonArray ja, FolderExporterContext context, String exportPath)
	{
		JsonArray res = new JsonArray();

		label: for (int i = 0; i < ja.size(); i++)
		{
			JsonObject doc = ja.getJsonObject(i);
			String fileId = doc.getString("_id");

			if (!context.errors.contains(fileId))
			{
				String filename = context.namesByIds.get(fileId);
				StringBuffer folders = new StringBuffer("");

				for (Map.Entry<String,List<JsonObject>> entry : context.docByFolders.entrySet())
				{
					String key = entry.getKey();
					List<JsonObject> values = entry.getValue();

					if (values.stream().anyMatch(jo -> fileId.equals(jo.getString("_id"))))
					{
						String[] s = key.split(exportPath,2);
						if (s.length < 2)
						{
							log.error("Documents : an error has occurred when mapping document " + fileId);
							continue label;
						}
						folders.append(s[1]);
						break;
					}
				}

				String localArchivePath = exportPath + folders.toString() + File.separator + filename;
				doc.put("localArchivePath", localArchivePath);
				res.add(doc);
			}
		}

		return res;
	}

	private void exportDocs(String exportPath, String locale, Map<String,JsonObject> queries,
							JsonArray cumulativeResults, Handler<Boolean> handler)
	{
		if (queries.isEmpty())
		{
			String filePath = exportPath + File.separator + I18n.getInstance().translate("workspace.title", I18n.DEFAULT_DOMAIN, locale);
			fs.writeFile(filePath, cumulativeResults.toBuffer(), new Handler<AsyncResult<Void>>()
			{
				@Override
				public void handle(AsyncResult<Void> event)
				{
					if (event.succeeded()) {
						handler.handle(true);
					} else {
						handler.handle(false);
					}
				}
			});
		}
		else
		{
			Map.Entry<String, JsonObject> entry = queries.entrySet().iterator().next();
			String folder = entry.getKey();
			JsonObject query = entry.getValue();
			queries.remove(folder);
			final String translatedFolder = StringUtils.stripAccents(I18n.getInstance().translate(folder, I18n.DEFAULT_DOMAIN, locale));
			final String exportPathFolder = exportPath + File.separator + translatedFolder;

			mongo.find(DocumentDao.DOCUMENTS_COLLECTION, query, new Handler<Message<JsonObject>>()
			{
				@Override
				public void handle(Message<JsonObject> event)
				{
					JsonArray results = event.body().getJsonArray("results");

					if ("ok".equals(event.body().getString("status")) && results != null)
					{
						List<JsonObject> rows = results.stream().map(obj -> (JsonObject) obj)
								.collect(Collectors.toList());
						exporter.export(new FolderExporterContext(exportPathFolder), rows).setHandler(res ->
						{
							if (res.succeeded())
							{
								JsonArray newResults = removeDuplicatesAndErrors(results, res.result(), translatedFolder);
								exportDocs(exportPath, locale, queries, cumulativeResults.addAll(newResults), handler);
							}
							else
							{
								log.error("Documents : Failed to export documents to " + exportPathFolder + " - " +
										res.cause());
								handler.handle(false);
							}
						});
					}
					else
					{
						log.error("Documents : Could not proceed query " + query.encode(),
								event.body().getString("message"));
						handler.handle(false);
					}
				}
			});
		}
	}

	@Override
	public void importResources(String importId, String userId, String userLogin, String userName, String importPath,
		String locale, boolean forceImportAsDuplication, Handler<JsonObject> handler)
	{
		WorkspaceRepositoryEvents self = this;

		final String backupPath = importPath + File.separator + I18n.getInstance().translate("workspace.title", I18n.DEFAULT_DOMAIN, locale);
		FolderImporterContext context = new FolderImporterContext(importPath, userId, userName);

		this.fs.readFile(backupPath, new Handler<AsyncResult<Buffer>>()
		{
			@Override
			public void handle(AsyncResult<Buffer> res)
			{
				Handler todo = new Handler<AsyncResult<Buffer>>()
				{
					@Override
					public void handle(AsyncResult<Buffer> res)
					{
						if(res.succeeded() == false)
							handler.handle(new JsonObject().put("status", "error").put("message", "Can't find main workspace file"));
						else
						{
							JsonArray filesBackup = res.result().toJsonArray();

							self.importer.importFoldersWorkspaceFormat(context, filesBackup, new Handler<JsonObject>()
							{
								@Override
								public void handle(JsonObject res)
								{
									JsonObject idsMap = new JsonObject();

									for(Map.Entry<String, String> entry : context.oldIdsToNewIds.entrySet())
										idsMap.put(entry.getKey(), entry.getValue());

									res
										.put("idsMap", new JsonObject()
											.put(DocumentDao.DOCUMENTS_COLLECTION, idsMap))
										.put("mainResourceName", DocumentDao.DOCUMENTS_COLLECTION);
									handler.handle(res);
								}
							});
						}
					}
				};

				if(res.succeeded() == false)
					self.fs.readFile(importPath + File.separator + "Documents", todo);
				else
					todo.handle(res);
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
