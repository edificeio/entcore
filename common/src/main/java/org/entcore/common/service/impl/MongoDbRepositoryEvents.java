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
import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.mongodb.MongoQueryBuilder;
import fr.wseduc.mongodb.MongoUpdateBuilder;
import org.entcore.common.mongodb.MongoDbConf;
import org.entcore.common.share.impl.MongoDbShareService;
import org.entcore.common.folders.impl.DocumentHelper;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.file.FileSystem;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileProps;
import io.vertx.core.Future;
import io.vertx.core.CompositeFuture;

import org.entcore.common.utils.FileUtils;
import org.entcore.common.utils.StringUtils;
import org.entcore.common.folders.FolderImporter;
import org.entcore.common.folders.FolderImporter.FolderImporterContext;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class MongoDbRepositoryEvents extends AbstractRepositoryEvents {

	protected static final Logger log = LoggerFactory.getLogger(MongoDbRepositoryEvents.class);
	protected final String managerRight;
	protected final String revisionsCollection;
	protected final String revisionIdAttribute;
	private final FolderImporter fileImporter;
	protected final Map<String, String> collectionNameToImportPrefixMap = new LinkedHashMap<String, String>();

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
		this.fileImporter = vertx == null ? null : new FolderImporter(vertx.fileSystem(), vertx.eventBus());
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
			fileName = StringUtils.replaceForbiddenCharacters(fileName);
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

	protected JsonObject revertExportChanges(JsonObject document, String importPrefix)
	{
		String title = DocumentHelper.getTitle(document);
		String name = DocumentHelper.getName(document);
		String headline = DocumentHelper.getAppProperty(document, "headline");

		if(title != null && title.startsWith(importPrefix) == true)
			DocumentHelper.setTitle(document, title.substring(importPrefix.length()));

		if(name != null && name.startsWith(importPrefix) == true)
			DocumentHelper.setName(document, name.substring(importPrefix.length()));

		if(headline != null && headline.startsWith(importPrefix) == true)
			DocumentHelper.setAppProperty(document, "headline", headline.substring(importPrefix.length()));

		return document;
	}

	@Override
	public void exportResources(JsonArray resourcesIds, String exportId, String userId, JsonArray g, String exportPath, String locale,
			String host, Handler<Boolean> handler)
	{
			QueryBuilder findByAuthor = QueryBuilder.start("author.userId").is(userId);
			QueryBuilder findByOwner = QueryBuilder.start("owner.userId").is(userId);
			QueryBuilder findByAuthorOrOwner = QueryBuilder.start().or(findByAuthor.get(), findByOwner.get());

			QueryBuilder findByShared = QueryBuilder.start().or(
				QueryBuilder.start("shared.userId").is(userId).get(),
				QueryBuilder.start("shared.groupId").in(g).get()
			);
			QueryBuilder findByAuthorOrOwnerOrShared = QueryBuilder.start().or(
				findByAuthorOrOwner.get(),
				findByShared.get()
			);

			JsonObject query;

			if(resourcesIds == null)
				query = MongoQueryBuilder.build(findByAuthorOrOwnerOrShared);
			else
			{
				QueryBuilder limitToResources = findByAuthorOrOwnerOrShared.and(
					QueryBuilder.start("_id").in(resourcesIds).get()
				);
				query = MongoQueryBuilder.build(limitToResources);
			}

			final AtomicBoolean exported = new AtomicBoolean(false);
			final String collection = MongoDbConf.getInstance().getCollection();

			mongo.find(collection, query, new Handler<Message<JsonObject>>()
			{
				@Override
				public void handle(Message<JsonObject> event)
				{
					JsonArray results = event.body().getJsonArray("results");
					if ("ok".equals(event.body().getString("status")) && results != null)
					{
						createExportDirectory(exportPath, locale, new Handler<String>()
						{
							@Override
							public void handle(String path)
							{
								if (path != null)
								{
									exportDocumentsDependancies(results, path, new Handler<Boolean>()
									{
										@Override
										public void handle(Boolean bool)
										{
											if (bool)
											{
												exportFiles(results, path, new HashSet<String>(), exported, handler);
											}
											else
											{
												// Should never happen, export doesn't fail if docs export fail.
												handler.handle(exported.get());
											}
										}
									});
								}
								else
								{
									handler.handle(exported.get());
								}
							}
						});
					}
					else
					{
						log.error(title + " : Could not proceed query " + query.encode(),
								event.body().getString("message"));
						handler.handle(exported.get());
					}
				}
			});
	}

	protected JsonObject sanitiseDocument(JsonObject document, String userId, String userName)
	{
		MongoDbCrudService.setUserMetadata(document, userId, userName);
		MongoDbShareService.removeShareMetadata(document);

		return document;
	}

	protected void readAllDocumentsFromDir(String dirPath, Handler<Map<String, JsonObject>> handler, String userId, String userName)
	{
		if(this.fileImporter == null)
				throw new RuntimeException("Cannot import documents without a file importer instance");

		MongoDbRepositoryEvents self = this;
		this.fs.readDir(dirPath, new Handler<AsyncResult<List<String>>>()
		{
			@Override
			public void handle(AsyncResult<List<String>> result)
			{
				if(result.succeeded() == false)
					throw new RuntimeException(result.cause());
				else
				{
					List<String> filesInDir = result.result();
					int nbFiles = filesInDir.size();

					ArrayList<JsonObject> mongoDocs = new ArrayList<JsonObject>(nbFiles);
					ArrayList<String> mongoDocsFileNames = new ArrayList<String>(nbFiles);
					AtomicInteger unprocessed = new AtomicInteger(nbFiles);
					AtomicInteger nbErrors = new AtomicInteger(0);

					for(int i = 0; i < nbFiles; ++i)
					{
						mongoDocs.add(null);
						mongoDocsFileNames.add(null);
					}

					List<FolderImporterContext> contexts = Collections.synchronizedList(new LinkedList<FolderImporterContext>());

					Handler finaliseRead = new Handler<Void>()
					{
						@Override
						public void handle(Void result)
						{
							for(FolderImporterContext importedCtx : contexts)
								self.fileImporter.applyFileIdsChange(importedCtx, mongoDocs);

							Map<String, JsonObject> fileMap = new HashMap<String, JsonObject>();
							for(int i = mongoDocs.size(); i-- > 0;)
								fileMap.put(mongoDocsFileNames.get(i), mongoDocs.get(i));

							handler.handle(fileMap);
						}
					};

					for(String filePath : filesInDir)
					{
						self.fs.props(filePath, new Handler<AsyncResult<FileProps>>()
						{
							@Override
							public void handle(AsyncResult<FileProps> propsResult)
							{
								if(propsResult.succeeded() == false)
									throw new RuntimeException(propsResult.cause());
								else
								{
									if(propsResult.result().isDirectory() == true)
									{
										FolderImporterContext ctx = new FolderImporterContext(filePath, userId, userName);
										self.fileImporter.importFoldersFlatFormat(ctx, new Handler<JsonObject>()
										{
											@Override
											public void handle(JsonObject rapport)
											{
												int ix = unprocessed.decrementAndGet();

												nbErrors.addAndGet(Integer.parseInt(rapport.getString("errorsNumber", "1")));
												contexts.add(ctx);

												if(ix == 0)
													finaliseRead.handle(null);
											}
										});
									}
									else
									{
										self.fs.readFile(filePath, new Handler<AsyncResult<Buffer>>()
										{
											@Override
											public void handle(AsyncResult<Buffer> fileResult)
											{
												if(fileResult.succeeded() == false)
													throw new RuntimeException(fileResult.cause());
												else
												{
													int ix = unprocessed.decrementAndGet();
													mongoDocs.set(ix, self.sanitiseDocument(fileResult.result().toJsonObject(), userId, userName));
													mongoDocsFileNames.set(ix, FileUtils.getFilename(filePath));

													if(ix == 0)
														finaliseRead.handle(null);
												}
											}
										});
									}
								}
							}
						});
					}
				}
			}
		});
	}

	public static void importDocuments(String collection, List<JsonObject> documents, Handler<JsonObject> handler)
	{
		if(documents.size() == 0)
		{
			JsonObject rapport =
						new JsonObject()
							.put("rapport",
								new JsonObject()
								.put("resourcesNumber", Integer.toString(0))
								.put("duplicatesNumber", Integer.toString(0))
								.put("errorsNumber", Integer.toString(0))
							)
							.put("idsMap", new JsonObject());

			handler.handle(rapport);
			return;
		}

		MongoDb mongo =  MongoDb.getInstance();

		JsonArray savePayload = new JsonArray();
		JsonArray idsToImport = new JsonArray();
		Map<String, Integer> idToIxMap = new HashMap<String, Integer>();
		Map<String, String> oldIdsToNewIds = new HashMap<String, String>();

		for(int i = 0, skipped = 0, l = documents.size(); i < l; ++i)
		{
			JsonObject d = documents.get(i);

			if(d == null)
			{
				++skipped;
				continue;
			}

			String docId = DocumentHelper.getId(d);

			savePayload.add(d);
			idsToImport.add(docId);
			idToIxMap.put(docId, i - skipped);
			oldIdsToNewIds.put(docId, docId);
		}

		QueryBuilder lookForExisting = QueryBuilder.start("_id").in(idsToImport);

		mongo.find(collection, MongoQueryBuilder.build(lookForExisting), new Handler<Message<JsonObject>>()
		{
			@Override
			public void handle(Message<JsonObject> searchMsg)
			{
				JsonObject body = searchMsg.body();

				if(body.getString("status").equals("ok"))
				{
					JsonArray foundDocs = body.getJsonArray("results");
					int nbDuplicates = 0;

					for(int i = foundDocs.size(); i-- > 0;)
					{
						String foundId = DocumentHelper.getId(foundDocs.getJsonObject(i));

						// Find already-existing resources
						Integer mapIx = idToIxMap.get(foundId);
						if(mapIx != null)
						{
							String newId = UUID.randomUUID().toString();
							oldIdsToNewIds.put(foundId, newId);
							// Create a duplicate
							DocumentHelper.setId(savePayload.getJsonObject(mapIx), newId);
							++nbDuplicates;
						}
					}

					final int totalDuplicates = nbDuplicates;
					mongo.insert(collection, savePayload, new Handler<Message<JsonObject>>()
					{
						@Override
						public void handle(Message<JsonObject> saveMsg)
						{
							JsonObject body = saveMsg.body();
							int nbErrors = -1;

							if(body.getString("status").equals("ok"))
								nbErrors = 0;
							else
							{
								JsonObject mongoError = new JsonObject(body.getString("message"));

								nbErrors = savePayload.size() - mongoError.getInteger("n");
							}

							Map<String, Object> convertedMap = new HashMap<String, Object>();
							convertedMap.putAll(oldIdsToNewIds);

							JsonObject rapport =
								new JsonObject()
									.put("rapport",
										new JsonObject()
										.put("resourcesNumber", Integer.toString(savePayload.size() - nbErrors))
										.put("duplicatesNumber", Integer.toString(totalDuplicates))
										.put("errorsNumber", Integer.toString(nbErrors))
									)
									.put("idsMap", new JsonObject(convertedMap));

							handler.handle(rapport);
						}
					});
				}
				else
				{
					// If the find fails, don't import anything
					JsonObject rapport =
						new JsonObject()
							.put("rapport",
								new JsonObject()
								.put("resourcesNumber", Integer.toString(0))
								.put("duplicatesNumber", Integer.toString(0))
								.put("errorsNumber", Integer.toString(savePayload.size()))
							)
							.put("idsMap", new JsonObject());

					handler.handle(rapport);
				}
			}
		});
	}

	@Override
	public void importResources(String importId, String userId, String userLogin, String userName, String importPath,
		String locale, Handler<JsonObject> handler)
	{
		MongoDbRepositoryEvents self = this;

		this.readAllDocumentsFromDir(importPath, new Handler<Map<String, JsonObject>>()
		{
			@Override
			public void handle(Map<String, JsonObject> docs)
			{
				Map<String, String> prefixMap = self.collectionNameToImportPrefixMap;

				// Single collection case, aka the generic mongoDB apps
				if(prefixMap.size() == 0)
				{
					prefixMap = new HashMap<String, String>();
					prefixMap.put(MongoDbConf.getInstance().getCollection(), "");
				}

				List<Future> collFutures = new LinkedList<Future>();
				List<Future> collFuturesChain = new LinkedList<Future>();

				/**
						Collections depending on others are expected to be bigger than the collections they depend on.
						e.g. Forum posts depend on their thread and there are more forum posts than there are forum threads.

						Thus, it is worth the extra memory avoiding to loop multiple times on the depending collections for id changes.
					*/
				Map<String, String> previousIdsMapCombined = new HashMap<String, String>();
				// Should be Map<String, Map<String, String>> but casting to JsonObject fails...
				Map<String, Map<String, Object>> previousIdsMapByCollection = new HashMap<String, Map<String, Object>>();

				String mainResourceName = "";

				for(Map.Entry<String, String> prefix : prefixMap.entrySet())
				{
					if(mainResourceName.equals("") == true)
							mainResourceName = prefix.getKey();
					ArrayList<JsonObject> collectionDocs = new ArrayList<JsonObject>(docs.size());

					// Sort documents depending on their collection
					for(Map.Entry<String, JsonObject> entry : docs.entrySet())
					{
						if(entry == null || entry.getKey() == null || entry.getValue() == null)
								continue;

						if(entry.getKey().startsWith(prefix.getValue()) == true)
							collectionDocs.add(self.revertExportChanges(entry.getValue(), prefix.getValue()));
					}

					if(collectionDocs.size() != 0)
					{
						Future<JsonObject> collDone = Future.future();
						Future<JsonObject> collChain = Future.future();

						// Import collections one by one because we might need to apply id changes
						Handler importNextCollection = new Handler<AsyncResult<JsonObject>>()
						{
							@Override
							public void handle(AsyncResult<JsonObject> previousResult)
							{
								if(previousResult != null)
								{
									// Replace old ids with new ids
									for(int i = collectionDocs.size(); i-- > 0;)
										AbstractRepositoryEvents.applyIdsChange(collectionDocs.get(i), previousIdsMapCombined);
								}

								MongoDbRepositoryEvents.importDocuments(prefix.getKey(), collectionDocs, new Handler<JsonObject>()
								{
									@Override
									public void handle(JsonObject result)
									{
										JsonObject previousIdsMap = result.getJsonObject("idsMap");

										Map<String, Object> collectionIdsMap = new HashMap<String, Object>();

										// Fill the ids maps
										for(String key : previousIdsMap.getMap().keySet())
										{
											String newId = previousIdsMap.getString(key);
											collectionIdsMap.put(key, newId);
											previousIdsMapCombined.put(key, newId);
										}

										previousIdsMapByCollection.put(prefix.getKey(), collectionIdsMap);

										collChain.complete(result);
										collDone.complete(result);
									}
								});
							}
						};

						// Start importing collections
						if(collFutures.size() == 0)
							importNextCollection.handle(null);
						else
							collFuturesChain.get(collFuturesChain.size() - 1).setHandler(importNextCollection);

						collFuturesChain.add(collChain);
						collFutures.add(collDone);
					}
				}

				final String mainResourceNameFinal = mainResourceName;

				// Fuse reports into a final one
				CompositeFuture.join(collFutures).setHandler(new Handler<AsyncResult<CompositeFuture>>()
				{
					@Override
					public void handle(AsyncResult<CompositeFuture> result)
					{
						if(result.succeeded() == true)
						{
							List<JsonObject> rapports = result.result().list();

							int nbResources = 0;
							int nbDuplicates = 0;
							int nbErrors = 0;

							for(JsonObject rapWrapper : rapports)
							{
								JsonObject rap = rapWrapper.getJsonObject("rapport");
								nbResources += Integer.parseInt(rap.getString("resourcesNumber"));
								nbDuplicates += Integer.parseInt(rap.getString("duplicatesNumber"));
								nbErrors += Integer.parseInt(rap.getString("errorsNumber"));
							}

							JsonObject idsMapObj = new JsonObject();
							for(Map.Entry<String, Map<String, Object>> entry : previousIdsMapByCollection.entrySet())
								idsMapObj.put(entry.getKey(), new JsonObject(entry.getValue()));

							JsonObject finalRapport =
								new JsonObject()
									.put("resourcesNumber", Integer.toString(nbResources))
									.put("duplicatesNumber", Integer.toString(nbDuplicates))
									.put("errorsNumber", Integer.toString(nbErrors))
									.put("resourcesIdsMap", idsMapObj)
									.put("mainResourceName", mainResourceNameFinal);

							handler.handle(finalRapport);
						}
						// Can't fail
					}
				});

			};
		}, userId, userName);
	}

}
