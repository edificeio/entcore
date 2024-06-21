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
import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;

import com.mongodb.client.model.Filters;
import io.vertx.core.AsyncResult;
import io.vertx.core.eventbus.DeliveryOptions;
import org.bson.conversions.Bson;
import org.entcore.common.folders.ElementQuery;
import org.entcore.common.folders.FolderExporter;
import org.entcore.common.folders.FolderExporter.FolderExporterContext;
import org.entcore.common.folders.FolderImporter;
import org.entcore.common.folders.FolderImporter.FolderImporterContext;
import org.entcore.common.folders.FolderManager;
import org.entcore.common.storage.Storage;
import org.entcore.common.user.RepositoryEvents;
import org.entcore.common.utils.StringUtils;
import org.entcore.workspace.controllers.WorkspaceController;
import org.entcore.workspace.dao.DocumentDao;


import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.mongodb.MongoQueryBuilder;
import fr.wseduc.mongodb.MongoUpdateBuilder;
import fr.wseduc.webutils.I18n;

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
	private final long timeout;

	private static final String SKIP_DOCUMENT_IMPORT_FILE = "skipDocs";

	public WorkspaceRepositoryEvents(Vertx vertx, Storage storage, boolean shareOldGroupsToUsers,
			FolderManager folderManager, FolderManager folderManagerRevision) {
		this.shareOldGroupsToUsers = shareOldGroupsToUsers;
		this.folderManager = folderManager;
		this.folderManagerRevision = folderManagerRevision;
		this.exporter = new FolderExporter(storage, vertx.fileSystem(), false);
		this.importer = new FolderImporter(vertx, vertx.fileSystem(), vertx.eventBus(), false);
		this.fs = vertx.fileSystem();
		this.timeout = vertx.getOrCreateContext().config().getLong("delete-users-timeout", 300000L);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void exportResources(final JsonArray resourcesIds, boolean exportDocuments, boolean exportSharedResources, final String exportId, final String userId,
								JsonArray groupIds, final String exportPathOrig, final String locale, String host, final Handler<Boolean> handler) {

		Bson findByOwner = Filters.eq("owner", userId);
		Bson findByShared = Filters.or(Filters.eq("inheritedShares.userId", userId),
				Filters.in("inheritedShares.groupId", groupIds));
		Bson findByOwnerOrShared = exportSharedResources == false ? findByOwner : Filters.or(findByOwner, findByShared);

		Bson findByOwnerOrSharedResourcesRestricted;

			if(resourcesIds == null)
				findByOwnerOrSharedResourcesRestricted = findByOwnerOrShared;
			else
			{
				findByOwnerOrSharedResourcesRestricted = Filters.and(findByOwnerOrShared,
					Filters.in("_id", resourcesIds)
				);
			}


		Bson isNotShared = Filters.or(
			Filters.exists("isShared", false),
			Filters.eq("isShared", false)
		);
		Bson isNotProtected = Filters.or(
			Filters.exists("protected", false),
			Filters.eq("protected", false)
		);
		Bson isNotPublic = Filters.or(
			Filters.exists("public", false),
			Filters.eq("public", false)
		);
		Bson isNotDeleted = Filters.or(
			Filters.exists("deleted", false),
			Filters.eq("deleted", false)
		);

		Bson myDocs = Filters.and(isNotShared,isNotProtected,isNotPublic,isNotDeleted);
		Bson sharedDocs = Filters.and(Filters.eq("isShared", true), isNotDeleted,isNotProtected,isNotPublic);
		Bson protectedDocs = Filters.and(Filters.eq("protected", true), isNotDeleted);
		Bson trashDocs = Filters.and(Filters.eq("deleted", true), Filters.eq("trasher", userId));

		final JsonObject queryMyDocs = MongoQueryBuilder.build(Filters.and(findByOwnerOrSharedResourcesRestricted,myDocs));
		final JsonObject querySharedDocs = MongoQueryBuilder.build(Filters.and(findByOwnerOrSharedResourcesRestricted,sharedDocs));
		final JsonObject queryProtectedDocs = MongoQueryBuilder.build(Filters.and(findByOwnerOrSharedResourcesRestricted,protectedDocs));
		final JsonObject queryTrashDocs = MongoQueryBuilder.build(Filters.and(findByOwnerOrSharedResourcesRestricted,trashDocs));

		final Map<String,JsonObject> queries = new HashMap<>();
		queries.put("documents",queryMyDocs);
		queries.put("shared",querySharedDocs);
		queries.put("appDocuments",queryProtectedDocs);
		queries.put("trash",queryTrashDocs);

		final String exportPath = exportPathOrig + File.separator + "Documents_tmp";
		final String finalExportPath = exportPathOrig + File.separator +
				I18n.getInstance().translate("workspace.title", I18n.DEFAULT_DOMAIN, locale);

		exportDocs(exportPath, exportDocuments, locale, queries, new JsonArray(), new Handler<Boolean>() {
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

	private void exportDocs(String exportPath, boolean exportDocuments, String locale, Map<String,JsonObject> queries,
							JsonArray cumulativeResults, Handler<Boolean> handler)
	{
		if (queries.isEmpty())
		{
			String filePath = exportPath + File.separator + I18n.getInstance().translate("workspace.title", I18n.DEFAULT_DOMAIN, locale);
			Handler<AsyncResult<Void>> finish = new Handler<AsyncResult<Void>>()
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
			};

			fs.writeFile(filePath, cumulativeResults.toBuffer(), new Handler<AsyncResult<Void>>()
			{
				@Override
				public void handle(AsyncResult<Void> event)
				{
					if(exportDocuments == true)
						finish.handle(event);
					else
						fs.writeFile(exportPath + File.separator + SKIP_DOCUMENT_IMPORT_FILE, new JsonObject().toBuffer(), finish);
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
						if(exportDocuments == false)
							rows = new ArrayList<JsonObject>();
						exporter.export(new FolderExporterContext(exportPathFolder), rows).onComplete(res ->
						{
							if (res.succeeded())
							{
								JsonArray newResults = removeDuplicatesAndErrors(results, res.result(), translatedFolder);
								exportDocs(exportPath, exportDocuments, locale, queries, cumulativeResults.addAll(newResults), handler);
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
		String locale, String host, boolean forceImportAsDuplication, Handler<JsonObject> handler)
	{
		final String skipDocs = importPath + File.separator + SKIP_DOCUMENT_IMPORT_FILE;
		this.fs.readFile(skipDocs, new Handler<AsyncResult<Buffer>>()
		{
			@Override
			public void handle(AsyncResult<Buffer> res)
			{
				importResources(importId, res.succeeded() == true, userId, userLogin, userName, importPath, locale, host, forceImportAsDuplication, handler);
			}
		});
	}

	public void importResources(String importId, boolean skipDocs, String userId, String userLogin, String userName, String importPath,
		String locale, String host, boolean forceImportAsDuplication, Handler<JsonObject> handler)
	{
		WorkspaceRepositoryEvents self = this;

		final String backupPath = importPath + File.separator + I18n.getInstance().translate("workspace.title", I18n.DEFAULT_DOMAIN, locale);
		FolderImporterContext context = new FolderImporterContext(importPath, userId, userName);
		context.setSkipDocumentImport(skipDocs);

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
										.put("resourcesIdsMap", new JsonObject()
											.put(DocumentDao.DOCUMENTS_COLLECTION, idsMap)
										)
										.put("duplicatesNumberMap", new JsonObject()
											.put(DocumentDao.DOCUMENTS_COLLECTION, res.getString("duplicatesNumber"))
										)
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
		if(groups == null)
			return;

		for(int i = groups.size(); i-- > 0;)
		{
			if(groups.hasNull(i))
				groups.remove(i);
			else if (groups.getJsonObject(i) != null && groups.getJsonObject(i).getString("group") == null)
				groups.remove(i);
		}
		if(groups.size() == 0)
			return;

		for (Object o : groups) {
			if (!(o instanceof JsonObject))
				continue;
			final JsonObject j = (JsonObject) o;
			final JsonObject query = MongoQueryBuilder
					.build(Filters.eq("inheritedShares.groupId", j.getString("group")));
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
				JsonArray userShare = new JsonArray();
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
		if(users == null)
			return;
		Set<String> userIds = new HashSet<>();
		for (int i = 0; i < users.size(); i++) {
			JsonObject j = users.getJsonObject(i);
			String id = j.getString("id");
			userIds.add(id);
		}
		for(int i = users.size(); i-- > 0;)
		{
			if(users.hasNull(i))
				users.remove(i);
			else if (users.getJsonObject(i) != null && users.getJsonObject(i).getString("id") == null)
				users.remove(i);
		}
		if(users.size() == 0)
			return;

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
		final JsonObject query = MongoQueryBuilder.build(Filters.in("shared.userId", userIds));
		JsonObject update = new JsonObject()
				.put("$pull",
						new JsonObject().put("shared",
								MongoQueryBuilder.build(Filters.in("userId", userIds))))//
				.put("$pull", new JsonObject().put("inheritedShares",
						MongoQueryBuilder.build(Filters.in("userId", userIds))));

		mongo.update(DocumentDao.DOCUMENTS_COLLECTION, query, update, false, true, null,
				new DeliveryOptions().setSendTimeout(timeout), event -> {
			if (!"ok".equals(event.body().getString("status"))) {
				log.error(event.body().getString("message"));
			}
		});
	}

}
